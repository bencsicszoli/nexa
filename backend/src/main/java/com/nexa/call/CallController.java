package com.nexa.call;

import com.nexa.call.dto.IceServersDto;
import com.nexa.subscription.SubscriptionRequired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A videohívás (#13) REST-végpontjai az {@code /api/calls} prefix alatt — hitelesítést igényel.
 * A jelzés (signaling) maga STOMP-on megy ({@link CallSignalController}); itt csak a kliensnek
 * szükséges ICE-szerver konfigurációt adjuk vissza.
 */
@RestController
@RequestMapping("/api/calls")
@SubscriptionRequired
public class CallController {

    private final CallService callService;

    public CallController(CallService callService) {
        this.callService = callService;
    }

    /** A {@code RTCPeerConnection}-höz használandó STUN/TURN szerverek. */
    @GetMapping("/ice-servers")
    public IceServersDto iceServers() {
        return callService.iceServers();
    }
}
