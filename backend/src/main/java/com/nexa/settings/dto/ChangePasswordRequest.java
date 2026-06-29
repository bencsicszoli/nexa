package com.nexa.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Jelszóváltás (#17): a jelenlegi jelszó ellenőrzéshez + az új jelszó (min. 8 karakter). */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword) {
}
