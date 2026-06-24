package com.nexa.auth.dto;

import com.nexa.user.User;

import java.time.Instant;

/** A frontendnek visszaadott felhasználói adat (jelszó-hash nélkül). */
public record UserDto(
        String id,
        String email,
        String displayName,
        String role,
        Instant createdAt) {

    public static UserDto from(User user) {
        return new UserDto(
                user.getId().toString(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getCreatedAt());
    }
}
