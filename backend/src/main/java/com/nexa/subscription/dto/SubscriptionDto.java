package com.nexa.subscription.dto;

import com.nexa.subscription.Subscription;
import com.nexa.subscription.SubscriptionAccess;
import com.nexa.subscription.SubscriptionStatus;

import java.time.Instant;

/**
 * Az előfizetés állapota a frontendnek. Bizalmas Paddle azonosítókat nem ad ki;
 * csak a megjelenítéshez/gatinghez szükséges mezőket. A {@code hasAccess} a backend-szabály
 * ({@link SubscriptionAccess}) eredménye — a frontend ezt használja a paywallhoz, nem számolja újra.
 */
public record SubscriptionDto(
        String status,
        String plan,
        Instant trialEndsAt,
        Instant renewsAt,
        Instant canceledAt,
        boolean hasAccess) {

    public static SubscriptionDto from(Subscription s) {
        return new SubscriptionDto(
                s.getStatus().name(),
                s.getPlan() != null ? s.getPlan().name() : null,
                s.getTrialEndsAt(),
                s.getRenewsAt(),
                s.getCanceledAt(),
                SubscriptionAccess.hasAccess(s.getStatus(), s.getTrialEndsAt(), Instant.now()));
    }

    /** Üres (NONE) állapot, amikor a felhasználónak még nincs előfizetési sora. */
    public static SubscriptionDto none() {
        return new SubscriptionDto(SubscriptionStatus.NONE.name(), null, null, null, null, false);
    }
}
