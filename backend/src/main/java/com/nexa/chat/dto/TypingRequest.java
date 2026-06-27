package com.nexa.chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** A kliens gépelés-jelzése egy szálban (STOMP {@code /app/chat.typing}). */
public record TypingRequest(@NotNull UUID conversationId) {
}
