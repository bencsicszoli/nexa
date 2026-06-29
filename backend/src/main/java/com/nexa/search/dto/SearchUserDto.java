package com.nexa.search.dto;

import com.nexa.user.User;

/**
 * Egy felhasználó-találat a keresőben (#16): a megjelenítéshez és a profilra navigáláshoz
 * elég adat, kapcsolatállapot nélkül (azt a profiloldal tölti be a találatra kattintva — így
 * a kereső nem fut N+1 kapcsolat-lekérdezésbe a sok találaton).
 */
public record SearchUserDto(
        String id,
        String displayName,
        String avatarUrl,
        String bio) {

    public static SearchUserDto of(User user) {
        return new SearchUserDto(
                user.getId().toString(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio());
    }
}
