package com.nexa.auth.dto;

/**
 * A 2FA beállítás indításának válasza (#17): a Base32 titok (kézi bevitelhez) és az
 * {@code otpauth://} URI, amiből a frontend a QR-kódot rajzolja. A 2FA ekkor még NINCS
 * bekapcsolva — az {@code /enable} egy érvényes kóddal aktiválja.
 */
public record TwoFactorSetupResponse(String secret, String otpauthUri) {
}
