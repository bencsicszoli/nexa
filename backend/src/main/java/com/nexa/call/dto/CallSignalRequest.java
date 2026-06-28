package com.nexa.call.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexa.call.CallSignalType;

import jakarta.validation.constraints.NotNull;

/**
 * A kliens által küldött WebRTC-jelzés (STOMP {@code /app/call.signal}). A {@code sdp} és a
 * {@code candidate} a böngésző natív {@code RTCSessionDescriptionInit}/{@code RTCIceCandidateInit}
 * objektuma — a backend nem értelmezi, csak átengedi ({@link JsonNode} pass-through), ezért a
 * jelzésprotokoll a frontend változásakor sem igényel backend-módosítást.
 */
public record CallSignalRequest(
        @NotNull String conversationId,
        @NotNull CallSignalType type,
        JsonNode sdp,
        JsonNode candidate) {
}
