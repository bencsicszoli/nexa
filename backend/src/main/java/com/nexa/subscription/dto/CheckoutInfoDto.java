package com.nexa.subscription.dto;

/**
 * A frontend Paddle overlay-checkoutjához szükséges (nem titkos) adatok.
 * A {@code clientToken} publikus; az aktuális felhasználó e-mailjét és a price
 * ID-kat a kliens a {@code Paddle.Checkout.open(...)} hívásban használja.
 */
public record CheckoutInfoDto(
        String environment,
        String clientToken,
        String priceMonthly,
        String priceAnnual,
        String customerEmail) {
}
