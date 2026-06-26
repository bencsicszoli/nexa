package com.nexa.realtime.dto;

import java.time.Instant;

/**
 * A klienshez tolt valós idejű értesítés. Egyelőre egyféle típus van: új bejegyzés
 * egy kapcsolattól ({@code NEW_POST}). Az értesítés nem perzisztens (live-only push):
 * a megnyitott munkamenetnek szól, kattintásra a kliens frissíti a hírfolyamot.
 *
 * <p>A {@code group*} mezők csak csoport-bejegyzésnél vannak kitöltve (a forráscsoport
 * jelzéséhez), profil-posztnál {@code null}.
 */
public record NotificationDto(
        String id,
        String type,
        String postId,
        String actorId,
        String actorName,
        String actorAvatarUrl,
        String groupId,
        String groupName,
        String groupLogoUrl,
        Instant createdAt) {

    public static final String TYPE_NEW_POST = "NEW_POST";
}
