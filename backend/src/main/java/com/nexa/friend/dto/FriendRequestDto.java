package com.nexa.friend.dto;

import com.nexa.user.User;

import java.time.Instant;

/**
 * Egy függőben lévő ismerőskérés a megjelenítéshez. A {@code requestId} az elfogadáshoz /
 * elutasításhoz / visszavonáshoz kell; az {@code user*} mezők a kérésben érintett másik fél.
 */
public record FriendRequestDto(
        String requestId,
        String userId,
        String displayName,
        String avatarUrl,
        String bio,
        Instant createdAt) {

    public static FriendRequestDto of(String requestId, User other, Instant createdAt) {
        return new FriendRequestDto(
                requestId,
                other.getId().toString(),
                other.getDisplayName(),
                other.getAvatarUrl(),
                other.getBio(),
                createdAt);
    }
}
