package com.nexa.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Üzenetküldés törzse — a STOMP {@code /app/chat.send} és a REST tartalék-végpont is ezt
 * használja. A szöveg hossza a {@code chat_messages.content} korlátjához igazodik (4000).
 */
public record SendMessageRequest(
        @NotNull UUID conversationId,
        @NotBlank @Size(max = 4000) String content) {
}
