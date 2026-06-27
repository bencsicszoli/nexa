package com.nexa.chat.dto;

import java.util.List;

/**
 * Egy lapnyi üzenet-előzmény (#12), időrendben (legrégebbi elöl — így a kliens egyből
 * megjelenítheti). A {@code nextCursor} a régebbi üzenetek betöltéséhez; {@code null}, ha
 * nincs több. Maga a beszélgetés metaadata is itt jön, hogy a szál fejléce egy hívásból kész.
 */
public record MessagesPageDto(
        ConversationDto conversation,
        List<ChatMessageDto> messages,
        String nextCursor) {
}
