package com.nexa.follow.dto;

import com.nexa.user.User;

import java.time.Instant;

/**
 * Egy követett vagy követő felhasználó a listák megjelenítéséhez: a kapcsolatban érintett
 * másik fél + a követés kezdetének időpontja.
 */
public record FollowUserDto(
        String id,
        String displayName,
        String avatarUrl,
        String bio,
        Instant since) {

    public static FollowUserDto of(User user, Instant since) {
        return new FollowUserDto(
                user.getId().toString(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                since);
    }
}
