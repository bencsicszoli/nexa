package com.nexa.chat;

import com.nexa.chat.dto.PresenceEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Online jelenlét nyilvántartása (#12). Egy felhasználó akkor „online", ha van legalább egy
 * élő STOMP-munkamenete (több fül / eszköz is lehet egyszerre). A {@link PresenceEventListener}
 * a STOMP CONNECT/DISCONNECT eseményekre hívja a {@link #connected}/{@link #disconnected}
 * metódusokat; az állapotváltáskor a {@code /topic/presence} csatornán szólunk a klienseknek,
 * hogy a beszélgetéslista pöttye élőben frissüljön.
 *
 * <p><b>Skálázás:</b> a számláló in-memory — egy backend-példánynál pontos. Több példány
 * esetén a jelenlét Redisbe (pl. {@code SETEX} + pub/sub) költöztethető, ugyanúgy, ahogy a
 * {@code WebSocketConfig} a {@code /topic}-fan-out Redis-relayét vázolja; ezt csak a mérés
 * indokolja, ezért most nem vezetjük be (lásd CLAUDE.md).
 */
@Service
public class PresenceService {

    private static final String PRESENCE_TOPIC = "/topic/presence";

    /** userId → élő munkamenetek száma. A 0-ra csökkenő bejegyzést eltávolítjuk. */
    private final ConcurrentHashMap<UUID, Integer> sessionCounts = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate messaging;

    public PresenceService(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    /** Egy munkamenet csatlakozott. Ha ezzel a felhasználó online lett, állapotváltást szórunk. */
    public void connected(UUID userId) {
        boolean becameOnline = sessionCounts.merge(userId, 1, Integer::sum) == 1;
        if (becameOnline) {
            broadcast(userId, true);
        }
    }

    /** Egy munkamenet lecsatlakozott. Ha ezzel a felhasználó offline lett, állapotváltást szórunk. */
    public void disconnected(UUID userId) {
        boolean becameOffline = sessionCounts.merge(userId, -1,
                (cur, delta) -> cur + delta <= 0 ? null : cur + delta) == null;
        if (becameOffline) {
            broadcast(userId, false);
        }
    }

    public boolean isOnline(UUID userId) {
        return sessionCounts.containsKey(userId);
    }

    /** A megadott id-k közül az online felhasználók halmaza (a lista-DTO-k feltöltéséhez). */
    public Set<UUID> onlineAmong(Set<UUID> userIds) {
        return userIds.stream().filter(sessionCounts::containsKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void broadcast(UUID userId, boolean online) {
        messaging.convertAndSend(PRESENCE_TOPIC, new PresenceEvent(userId.toString(), online));
    }
}
