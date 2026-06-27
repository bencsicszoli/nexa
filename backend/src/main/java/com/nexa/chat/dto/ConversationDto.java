package com.nexa.chat.dto;

import java.time.Instant;

/**
 * Egy beszélgetés a listához és a szál fejlécéhez (#12). A {@code title}/{@code imageUrl} a
 * szál fajtája szerint a másik fél (DIRECT) vagy a csoport (GROUP) adata; az {@code online}
 * csak DIRECT-nél értelmezett (a másik fél jelenléte).
 */
public record ConversationDto(
        String id,
        String type,
        String title,
        String imageUrl,
        /** DIRECT szálnál a másik fél id-ja (profilhoz/jelenléthez); GROUP-nál {@code null}. */
        String otherUserId,
        /** GROUP szálnál a csoport id-ja (csoportoldalhoz); DIRECT-nél {@code null}. */
        String groupId,
        /** A másik fél online-e (csak DIRECT-nél; GROUP-nál mindig {@code false}). */
        boolean online,
        /** A legutóbbi üzenet szöveges előnézete (vagy {@code null}, ha még nincs üzenet). */
        String lastMessagePreview,
        Instant lastMessageAt,
        long unreadCount) {
}
