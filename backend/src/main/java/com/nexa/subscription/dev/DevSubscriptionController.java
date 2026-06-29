package com.nexa.subscription.dev;

import com.nexa.common.ApiException;
import com.nexa.subscription.Plan;
import com.nexa.subscription.Subscription;
import com.nexa.subscription.SubscriptionRepository;
import com.nexa.subscription.SubscriptionStatus;
import com.nexa.subscription.dto.SubscriptionDto;
import com.nexa.user.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * FEJLESZTŐI/DEMO végpont (#15) — csak {@code nexa.payment.dev-controls=true} esetén létezik a bean
 * (egyébként a route 404, így élesben nem elérhető). Élő Paddle nélkül állítja be a bejelentkezett
 * felhasználó előfizetés-állapotát, hogy a paywall megjelenése/eltűnése demózható és tesztelhető legyen.
 *
 * <p>Hitelesítést igényel (a {@code /api/**} alatt a JWT-szűrő védi), de NEM
 * {@code @SubscriptionRequired} — különben egy paywall mögé zárt (NONE) user nem tudná kiengedni magát.
 */
@RestController
@RequestMapping("/api/dev/subscription")
@ConditionalOnProperty(name = "nexa.payment.dev-controls", havingValue = "true")
public class DevSubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public DevSubscriptionController(SubscriptionRepository subscriptionRepository,
                                     UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    /** A bejelentkezett felhasználó aktuális állapota (a frontend-panel ezzel detektálja a flag aktív voltát). */
    @GetMapping
    @Transactional(readOnly = true)
    public SubscriptionDto current(@AuthenticationPrincipal UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(SubscriptionDto::from)
                .orElseGet(SubscriptionDto::none);
    }

    /** Beállítja a megadott állapotot (és a hozzá illő dátumokat). */
    @PostMapping
    @Transactional
    public SubscriptionDto set(@AuthenticationPrincipal UUID userId, @RequestBody DevSubscriptionRequest request) {
        SubscriptionStatus status = parseStatus(request.status());
        if (userRepository.findById(userId).isEmpty()) {
            throw ApiException.userNotFound();
        }

        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> new Subscription(userId));

        Instant now = Instant.now();
        int days = request.trialDaysFromNow() != null ? request.trialDaysFromNow() : 7;
        Plan requestedPlan = parsePlan(request.plan());

        sub.setStatus(status);
        switch (status) {
            case TRIALING -> {
                if (sub.getPlan() == null) sub.setPlan(Plan.MONTHLY);
                Instant trialEnd = now.plus(days, ChronoUnit.DAYS);
                sub.setTrialEndsAt(trialEnd);
                sub.setRenewsAt(trialEnd);
                sub.setCanceledAt(null);
            }
            case ACTIVE -> {
                if (sub.getPlan() == null) sub.setPlan(Plan.MONTHLY);
                sub.setTrialEndsAt(null);
                sub.setRenewsAt(now.plus(30, ChronoUnit.DAYS));
                sub.setCanceledAt(null);
            }
            case PAST_DUE -> {
                if (sub.getPlan() == null) sub.setPlan(Plan.MONTHLY);
                sub.setRenewsAt(now.plus(3, ChronoUnit.DAYS));
                sub.setCanceledAt(null);
            }
            case PAUSED -> sub.setCanceledAt(null);
            case CANCELED -> sub.setCanceledAt(now);
            case NONE -> {
                sub.setPlan(null);
                sub.setTrialEndsAt(null);
                sub.setRenewsAt(null);
                sub.setCanceledAt(null);
            }
        }
        // A kérésben megadott csomag (ha van és nem NONE) felülírja az alapértelmezést — a demóban
        // a választott Havi/Éves tükröződjön.
        if (requestedPlan != null && status != SubscriptionStatus.NONE) {
            sub.setPlan(requestedPlan);
        }
        sub.touch();
        Subscription saved = subscriptionRepository.save(sub);
        return SubscriptionDto.from(saved);
    }

    private static SubscriptionStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS", "Missing subscription status.");
        }
        try {
            return SubscriptionStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS", "Unknown subscription status: " + raw);
        }
    }

    /** Opcionális csomag a kéréshez (null/ismeretlen → nem módosít). */
    private static Plan parsePlan(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Plan.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * A dev-állapotbeállítás kérése: kötelező {@code status}, opcionális {@code trialDaysFromNow}
     * és {@code plan} (MONTHLY/ANNUAL).
     */
    public record DevSubscriptionRequest(String status, Integer trialDaysFromNow, String plan) {
    }
}
