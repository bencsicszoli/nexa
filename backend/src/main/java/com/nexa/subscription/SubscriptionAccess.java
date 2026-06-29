package com.nexa.subscription;

import java.time.Instant;

/**
 * Az előfizetés-hozzáférés (entitlement) egyetlen forrása (#15). Tiszta, mellékhatás-mentes
 * szabály, hogy a backend-guard ({@link SubscriptionGuardInterceptor}) és a frontendnek kiadott
 * {@link com.nexa.subscription.dto.SubscriptionDto} ugyanazt a döntést hozza.
 *
 * <p>Szabály:
 * <ul>
 *   <li>{@code ACTIVE} → hozzáfér;</li>
 *   <li>{@code PAST_DUE} → hozzáfér (grace — a Paddle újrapróbálja a terhelést, fizető usert
 *       dunning közben nem zárunk ki);</li>
 *   <li>{@code TRIALING} és a próbaidő még tart ({@code trialEndsAt} a jövőben) → hozzáfér;</li>
 *   <li>{@code NONE}, {@code PAUSED}, {@code CANCELED}, illetve lejárt trial → nincs hozzáférés (paywall).</li>
 * </ul>
 */
public final class SubscriptionAccess {

    private SubscriptionAccess() {
    }

    public static boolean hasAccess(SubscriptionStatus status, Instant trialEndsAt, Instant now) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case ACTIVE, PAST_DUE -> true;
            case TRIALING -> trialEndsAt != null && trialEndsAt.isAfter(now);
            case NONE, PAUSED, CANCELED -> false;
        };
    }
}
