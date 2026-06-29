package com.nexa.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexa.common.ApiException;
import com.nexa.subscription.dto.CheckoutInfoDto;
import com.nexa.subscription.dto.SubscriptionDto;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Az előfizetés üzleti logikája: állapot-lekérdezés, checkout-adatok kiadása,
 * portál-URL, és a Paddle webhookok feldolgozása (a DB az állapot tükre).
 * A Paddle az igazság forrása; visszaélés ellen a checkout kártyát kér.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PaddleWebhookVerifier webhookVerifier;
    private final PaddleClient paddleClient;
    private final PaddleProperties properties;
    private final ObjectMapper objectMapper;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            PaddleWebhookVerifier webhookVerifier,
            PaddleClient paddleClient,
            PaddleProperties properties,
            ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.webhookVerifier = webhookVerifier;
        this.paddleClient = paddleClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SubscriptionDto getMine(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(SubscriptionDto::from)
                .orElseGet(SubscriptionDto::none);
    }

    /**
     * Van-e a felhasználónak hozzáférése (aktív előfizetés vagy folyamatban lévő próbaidő)?
     * A gating-guard ({@link SubscriptionGuardInterceptor}) ezt hívja. Sor hiányában (NONE)
     * nincs hozzáférés. A szabály egyetlen forrása: {@link SubscriptionAccess}.
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(s -> SubscriptionAccess.hasAccess(s.getStatus(), s.getTrialEndsAt(), Instant.now()))
                .orElse(false);
    }

    /** A frontend overlay-checkouthoz szükséges (nem titkos) adatok. */
    @Transactional(readOnly = true)
    public CheckoutInfoDto checkoutInfo(UUID userId) {
        if (!properties.isConfigured()) {
            throw ApiException.subscriptionNotConfigured();
        }
        User user = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
        return new CheckoutInfoDto(
                properties.getEnvironment(),
                properties.getClientToken(),
                properties.getPriceMonthly(),
                properties.getPriceAnnual(),
                user.getEmail());
    }

    /** A Paddle számlázási portál URL-je az aktuális felhasználóhoz. */
    @Transactional(readOnly = true)
    public String portalUrl(UUID userId) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(ApiException::noSubscription);
        if (sub.getPaddleCustomerId() == null || sub.getPaddleCustomerId().isBlank()) {
            throw ApiException.noSubscription();
        }
        return paddleClient.createPortalSession(sub.getPaddleCustomerId());
    }

    /**
     * Egy Paddle webhook feldolgozása: aláírás-ellenőrzés → az érintett előfizetés
     * upsertelése a {@code data.status} alapján. Idempotens: ugyanazt az eseményt
     * többször megkapva ugyanarra az állapotra konvergál.
     */
    @Transactional
    public void handleWebhook(String rawBody, String signatureHeader) {
        webhookVerifier.verify(rawBody, signatureHeader);

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw ApiException.invalidWebhookSignature();
        }

        String eventType = root.path("event_type").asText("");
        if (!eventType.startsWith("subscription.")) {
            // Más eseménytípusokat (transaction.*, stb.) most nem kezelünk.
            log.debug("Figyelmen kívül hagyott Paddle esemény: {}", eventType);
            return;
        }

        JsonNode data = root.path("data");
        String paddleSubscriptionId = textOrNull(data, "id");
        String customerId = textOrNull(data, "customer_id");
        String paddleStatus = data.path("status").asText("");
        UUID userId = parseUserId(data.path("custom_data").path("userId").asText(null));

        Subscription sub = resolveSubscription(paddleSubscriptionId, userId, customerId);
        if (sub == null) {
            log.warn("Nem köthető felhasználóhoz a Paddle webhook (sub={}, customer={})",
                    paddleSubscriptionId, customerId);
            return;
        }

        if (paddleSubscriptionId != null) {
            sub.setPaddleSubscriptionId(paddleSubscriptionId);
        }
        if (customerId != null) {
            sub.setPaddleCustomerId(customerId);
        }
        applyPlan(sub, data);
        applyStatus(sub, paddleStatus, data);
        sub.touch();
        subscriptionRepository.save(sub);
        log.info("Előfizetés frissítve webhookból: user={}, status={}", sub.getUserId(), sub.getStatus());
    }

    /** Megkeresi (vagy létrehozza) az eseményhez tartozó előfizetési sort. */
    private Subscription resolveSubscription(String paddleSubscriptionId, UUID userId, String customerId) {
        if (paddleSubscriptionId != null) {
            Optional<Subscription> bySub = subscriptionRepository.findByPaddleSubscriptionId(paddleSubscriptionId);
            if (bySub.isPresent()) {
                return bySub.get();
            }
        }
        if (userId != null) {
            return subscriptionRepository.findByUserId(userId)
                    .orElseGet(() -> userRepository.findById(userId)
                            .map(u -> new Subscription(u.getId()))
                            .orElse(null));
        }
        if (customerId != null) {
            return subscriptionRepository.findByPaddleCustomerId(customerId).orElse(null);
        }
        return null;
    }

    private void applyStatus(Subscription sub, String paddleStatus, JsonNode data) {
        switch (paddleStatus) {
            case "trialing" -> {
                sub.setStatus(SubscriptionStatus.TRIALING);
                Instant trialEnd = parseInstant(data.path("trial_dates").path("ends_at").asText(null));
                if (trialEnd == null) {
                    trialEnd = parseInstant(textOrNull(data, "next_billed_at"));
                }
                sub.setTrialEndsAt(trialEnd);
                sub.setRenewsAt(parseInstant(textOrNull(data, "next_billed_at")));
                sub.setCanceledAt(null);
            }
            case "active" -> {
                sub.setStatus(SubscriptionStatus.ACTIVE);
                sub.setRenewsAt(parseInstant(textOrNull(data, "next_billed_at")));
                sub.setCanceledAt(null);
            }
            case "past_due" -> {
                sub.setStatus(SubscriptionStatus.PAST_DUE);
                sub.setRenewsAt(parseInstant(textOrNull(data, "next_billed_at")));
            }
            case "paused" -> sub.setStatus(SubscriptionStatus.PAUSED);
            case "canceled" -> {
                sub.setStatus(SubscriptionStatus.CANCELED);
                Instant canceledAt = parseInstant(textOrNull(data, "canceled_at"));
                sub.setCanceledAt(canceledAt != null ? canceledAt : Instant.now());
            }
            default -> log.warn("Ismeretlen Paddle előfizetés-állapot: {}", paddleStatus);
        }
    }

    private void applyPlan(Subscription sub, JsonNode data) {
        JsonNode items = data.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return;
        }
        String priceId = items.get(0).path("price").path("id").asText(null);
        if (priceId == null) {
            return;
        }
        if (priceId.equals(properties.getPriceMonthly())) {
            sub.setPlan(Plan.MONTHLY);
        } else if (priceId.equals(properties.getPriceAnnual())) {
            sub.setPlan(Plan.ANNUAL);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return (value == null || value.isBlank() || "null".equals(value)) ? null : value;
    }

    private static UUID parseUserId(String raw) {
        if (raw == null || raw.isBlank() || "null".equals(raw)) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
