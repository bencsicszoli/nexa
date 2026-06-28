package com.nexa.call;

import com.nexa.call.dto.CallSignalRequest;
import com.nexa.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * A videohívás (#13) élő, STOMP-os jelzésbemenete. A kliens a {@code /app/call.signal} célra
 * küldi az ajánlatot/választ/ICE-jelölteket és a hívás-vezérlő kereteket; a hitelesített
 * felhasználót a STOMP CONNECT-kor beállított {@code StompPrincipal} adja (name = userId).
 *
 * <p>A hibákat — a csevegéshez hasonlóan ({@code ChatMessageController}) — itt elnyeljük: egy
 * elutasított jelzés (pl. nincs hozzáférés) ne szakítsa meg a kliens WebSocket-kapcsolatát.
 */
@Controller
public class CallSignalController {

    private static final Logger log = LoggerFactory.getLogger(CallSignalController.class);

    private final CallService callService;

    public CallSignalController(CallService callService) {
        this.callService = callService;
    }

    @MessageMapping("/call.signal")
    public void signal(@Payload CallSignalRequest request, Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        try {
            callService.relay(userId, request);
        } catch (ApiException e) {
            log.debug("STOMP hívás-jelzés elutasítva ({}): {}", e.getCode(), e.getMessage());
        }
    }
}
