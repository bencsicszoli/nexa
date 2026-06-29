package com.nexa.subscription;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@link SubscriptionAccess} tiszta entitlement-szabálya (#15), minden állapotra.
 */
class SubscriptionAccessTest {

    private final Instant now = Instant.parse("2026-06-29T12:00:00Z");
    private final Instant future = now.plus(7, ChronoUnit.DAYS);
    private final Instant past = now.minus(1, ChronoUnit.DAYS);

    @Test
    void activeAndPastDueHaveAccess() {
        assertThat(SubscriptionAccess.hasAccess(SubscriptionStatus.ACTIVE, null, now)).isTrue();
        assertThat(SubscriptionAccess.hasAccess(SubscriptionStatus.PAST_DUE, null, now)).isTrue();
    }

    @Test
    void trialingOnlyWhileTrialIsInTheFuture() {
        assertThat(SubscriptionAccess.hasAccess(SubscriptionStatus.TRIALING, future, now)).isTrue();
        assertThat(SubscriptionAccess.hasAccess(SubscriptionStatus.TRIALING, past, now)).isFalse();
        assertThat(SubscriptionAccess.hasAccess(SubscriptionStatus.TRIALING, null, now)).isFalse();
    }

    @Test
    void noneePausedCanceledHaveNoAccess() {
        assertThat(SubscriptionAccess.hasAccess(SubscriptionStatus.NONE, null, now)).isFalse();
        assertThat(SubscriptionAccess.hasAccess(SubscriptionStatus.PAUSED, future, now)).isFalse();
        assertThat(SubscriptionAccess.hasAccess(SubscriptionStatus.CANCELED, future, now)).isFalse();
        assertThat(SubscriptionAccess.hasAccess(null, future, now)).isFalse();
    }
}
