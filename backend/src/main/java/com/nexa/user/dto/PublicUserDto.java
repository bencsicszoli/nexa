package com.nexa.user.dto;

import com.nexa.user.User;

import java.time.Instant;

/**
 * Egy felhasználó nyilvános profilja a hívó szemszögéből (e-mail nélkül). A kapcsolat-mezők
 * a profilon megjeleníthető műveleteket vezérlik:
 * <ul>
 *   <li>{@code self} — a hívó saját profilja (a frontend ekkor a szerkeszthető profilra irányít),</li>
 *   <li>{@code friendStatus} — {@code NONE} / {@code FRIENDS} / {@code REQUEST_SENT} /
 *       {@code REQUEST_RECEIVED} (selfnél {@code SELF}),</li>
 *   <li>{@code friendRequestId} — a függő ismerőskérés azonosítója (elfogadáshoz / visszavonáshoz),</li>
 *   <li>{@code following} — a hívó követi-e a felhasználót.</li>
 * </ul>
 */
public record PublicUserDto(
        String id,
        String displayName,
        String avatarUrl,
        String bio,
        Instant createdAt,
        boolean self,
        String friendStatus,
        String friendRequestId,
        boolean following) {

    public static PublicUserDto of(User user, boolean self, String friendStatus,
                                   String friendRequestId, boolean following) {
        return new PublicUserDto(
                user.getId().toString(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getCreatedAt(),
                self,
                friendStatus,
                friendRequestId,
                following);
    }
}
