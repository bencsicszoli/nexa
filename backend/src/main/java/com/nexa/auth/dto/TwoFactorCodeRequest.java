package com.nexa.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Egy 2FA kód (TOTP vagy helyreállító) az enable/disable művelethez (#17). */
public record TwoFactorCodeRequest(@NotBlank String code) {
}
