package com.nexa.chat;

import com.nexa.user.UserRepository;
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
 *
 * <p>Aki elrejti a jelenlétét (#17, {@code hidePresence}), annál a CONNECT-re NEM jelzünk online-t —
 * így sehol (chat-lista, jelenlét-topic) nem látszik elérhetőnek. A flag menet közbeni változása a
 * következő (újra)csatlakozásnál érvényesül.
 */
@Component
public class PresenceEventListener {

    private final PresenceService presenceService;
    private final UserRepository userRepository;

    public PresenceEventListener(PresenceService presenceService, UserRepository userRepository) {
        this.presenceService = presenceService;
        this.userRepository = userRepository;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        userId(event.getUser()).ifPresent(id -> {
            boolean hidden = userRepository.findById(id)
                    .map(u -> u.isHidePresence())
                    .orElse(false);
            if (!hidden) {
                presenceService.connected(id);
            }
        });
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
