package com.nexa.subscription.dto;

import com.nexa.subscription.Subscription;
import com.nexa.subscription.SubscriptionStatus;

import java.time.Instant;

/**
 * Az előfizetés állapota a frontendnek. Bizalmas Paddle azonosítókat nem ad ki;
 * csak a megjelenítéshez/gatinghez szükséges mezőket.
 */
public record SubscriptionDto(
        String status,
        String plan,
        Instant trialEndsAt,
        Instant renewsAt,
        Instant canceledAt) {

    public static SubscriptionDto from(Subscription s) {
        return new SubscriptionDto(
                s.getStatus().name(),
                s.getPlan() != null ? s.getPlan().name() : null,
                s.getTrialEndsAt(),
                s.getRenewsAt(),
                s.getCanceledAt());
    }

    /** Üres (NONE) állapot, amikor a felhasználónak még nincs előfizetési sora. */
    public static SubscriptionDto none() {
        return new SubscriptionDto(SubscriptionStatus.NONE.name(), null, null, null, null);
    }
}
