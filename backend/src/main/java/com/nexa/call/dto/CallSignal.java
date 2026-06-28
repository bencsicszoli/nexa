package com.nexa.call.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexa.call.CallSignalType;

/**
 * A másik félnek továbbított WebRTC-jelzés (kimenő, {@code /user/queue/call}). A küldő
 * azonosítóját, nevét és avatárját is tartalmazza, hogy a bejövő hívás UI-ja (csörgés) a
 * hívót azonnal meg tudja jeleníteni — előzetes lekérés nélkül.
 */
public record CallSignal(
        String conversationId,
        String fromUserId,
        String fromName,
        String fromAvatarUrl,
        CallSignalType type,
        JsonNode sdp,
        JsonNode candidate) {
}
