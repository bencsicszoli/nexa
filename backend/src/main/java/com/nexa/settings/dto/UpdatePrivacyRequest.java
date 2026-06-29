package com.nexa.settings.dto;

/**
 * Adatvédelmi kapcsolók (#17): kereshetőség (felfedezés a keresőben) és az online jelenlét
 * elrejtése. A {@code hidePresence} változása a következő STOMP-újracsatlakozásnál érvényesül.
 */
public record UpdatePrivacyRequest(
        boolean searchable,
        boolean hidePresence) {
}
