package com.nexa.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** A kétlépcsős login második lépése (#17): a challenge token + a 2FA (TOTP vagy helyreállító) kód. */
public record Login2faRequest(
        @NotBlank String challengeToken,
        @NotBlank String code) {
}
