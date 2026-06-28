package com.nexa.subscription;

/**
 * Választható előfizetési csomag. A konkrét Paddle price ID-t a
 * {@code nexa.payment.paddle.price-monthly} / {@code price-annual} config adja.
 */
public enum Plan {
    MONTHLY,
    ANNUAL
}
