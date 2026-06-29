package com.nexa.subscription;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jelölő annotáció (#15): az így megjelölt controller-osztály vagy -metódus
 * aktív előfizetést / folyamatban lévő próbaidőt követel. Enélkül a
 * {@link SubscriptionGuardInterceptor} {@code 402 SUBSCRIPTION_REQUIRED}-et ad (paywall).
 * Osztályra téve a benne lévő összes végpontra hat.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SubscriptionRequired {
}
