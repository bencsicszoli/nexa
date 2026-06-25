package com.nexa.friend.dto;

import com.nexa.user.User;

/**
 * Egy felhasználó az „Emberek" böngészéséhez, a bejelentkezett felhasználóhoz viszonyított
 * kapcsolati állapottal együtt — így a UI a megfelelő műveletet ajánlhatja fel.
 * <p>
 * A {@code relationship} lehetséges értékei:
 * <ul>
 *     <li>{@code NONE} — nincs kapcsolat; küldhető kérés.</li>
 *     <li>{@code FRIENDS} — már ismerősök.</li>
 *     <li>{@code REQUEST_SENT} — a bejelentkezett felhasználó küldött kérést (visszavonható).</li>
 *     <li>{@code REQUEST_RECEIVED} — a másik fél küldött kérést (elfogadható).</li>
 * </ul>
 * A {@code requestId} csak függőben lévő kérésnél (SENT/RECEIVED) van kitöltve.
 */
public record UserSummaryDto(
        String id,
        String displayName,
        String avatarUrl,
        String bio,
        String relationship,
        String requestId) {

    public static UserSummaryDto of(User user, String relationship, String requestId) {
        return new UserSummaryDto(
                user.getId().toString(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                relationship,
                requestId);
    }
}
