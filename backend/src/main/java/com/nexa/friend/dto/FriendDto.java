package com.nexa.friend.dto;

import com.nexa.user.User;

import java.time.Instant;

/**
 * Egy elfogadott ismerős a megjelenítéshez: a kapcsolat „másik fele" + mióta ismerősök.
 */
public record FriendDto(
        String id,
        String displayName,
        String avatarUrl,
        String bio,
        Instant friendsSince) {

    public static FriendDto of(User other, Instant friendsSince) {
        return new FriendDto(
                other.getId().toString(),
                other.getDisplayName(),
                other.getAvatarUrl(),
                other.getBio(),
                friendsSince);
    }
}
