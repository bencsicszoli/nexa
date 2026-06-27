package com.nexa.chat;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

/**
 * A STOMP-munkamenetek élettartamát a {@link PresenceService}-hez köti (#12): a hitelesített
 * CONNECT-re online, a DISCONNECT-re (kapcsolatbontás, fülbezárás, lejárat) offline. A
 * felhasználót a CONNECT-kor beállított {@link StompPrincipal} azonosítja.
 */
@Component
public class PresenceEventListener {

    private final PresenceService presenceService;

    public PresenceEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        userId(event.getUser()).ifPresent(presenceService::connected);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        userId(event.getUser()).ifPresent(presenceService::disconnected);
    }

    private java.util.Optional<UUID> userId(Principal principal) {
        if (principal == null) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(UUID.fromString(principal.getName()));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }
}
