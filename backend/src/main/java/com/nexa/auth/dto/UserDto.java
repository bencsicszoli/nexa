package com.nexa.auth.dto;

import com.nexa.user.User;

import java.time.Instant;

/**
 * A frontendnek visszaadott felhasználói adat (jelszó-hash nélkül). A {@code locale} és a
 * {@code totpEnabled} (#17) additív mezők: a bejelentkezés/{@code /auth/me} ezekkel állítja be
 * a felület nyelvét és jelzi a 2FA állapotát.
 */
public record UserDto(
        String id,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        String role,
        String locale,
        boolean totpEnabled,
        Instant createdAt) {

    public static UserDto from(User user) {
        return new UserDto(
                user.getId().toString(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.getLocale(),
                user.isTotpEnabled(),
                user.getCreatedAt());
    }
}
