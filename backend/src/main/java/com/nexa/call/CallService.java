package com.nexa.call;

import com.nexa.call.dto.CallSignal;
import com.nexa.call.dto.CallSignalRequest;
import com.nexa.call.dto.IceServersDto;
import com.nexa.chat.Conversation;
import com.nexa.chat.ConversationParticipantRepository;
import com.nexa.chat.ConversationRepository;
import com.nexa.chat.ConversationType;
import com.nexa.common.ApiException;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Az 1:1 videohívás (#13) backend-logikája: a WebRTC-jelzés (SDP/ICE) <b>relézése</b> a hívás
 * két résztvevője között, illetve az ICE-szerver lista összeállítása a configból.
 *
 * <p>A szerver szándékosan <b>állapotmentes relé</b>: nem tárol hívás-állapotot, csak a kérő
 * hozzáférését ellenőrzi (a {@code DIRECT} szál résztvevője-e), megkeresi a másik felet, és neki
 * továbbítja a keretet a {@code /user/queue/call} úton. A hívás-állapotgépet (csörgés, elfogadás,
 * bontás) a kliensek tartják — ugyanaz a megosztott STOMP-kapcsolat hordozza, mint a csevegést.
 *
 * <p><b>Miért csak DIRECT:</b> a kártya 1:1 hívást ír elő; a csoport-hívás (több peer, SFU/mesh)
 * külön feladat, ezért a csoport-szálon érkező jelzést elutasítjuk.
 */
@Service
public class CallService {

    private static final String CALL_DEST = "/queue/call";

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messaging;

    private final List<String> stunUrls;
    private final String turnUrl;
    private final String turnUsername;
    private final String turnCredential;

    public CallService(ConversationRepository conversationRepository,
                       ConversationParticipantRepository participantRepository,
                       UserRepository userRepository,
                       SimpMessagingTemplate messaging,
                       @Value("${nexa.webrtc.stun-urls:}") String stunUrls,
                       @Value("${nexa.webrtc.turn-url:}") String turnUrl,
                       @Value("${nexa.webrtc.turn-username:}") String turnUsername,
                       @Value("${nexa.webrtc.turn-credential:}") String turnCredential) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.messaging = messaging;
        this.stunUrls = Arrays.stream(stunUrls.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.turnUrl = turnUrl == null ? "" : turnUrl.trim();
        this.turnUsername = turnUsername;
        this.turnCredential = turnCredential;
    }

    /**
     * Egy WebRTC-jelzés továbbítása a {@code DIRECT} szál másik résztvevőjének. A hozzáférést
     * ugyanúgy ellenőrizzük, mint a csevegésnél: idegen/nem létező szál esetén
     * {@code CONVERSATION_NOT_FOUND} (a létezést sem szivárogtatjuk).
     */
    @Transactional(readOnly = true)
    public void relay(UUID senderId, CallSignalRequest request) {
        UUID conversationId;
        try {
            conversationId = UUID.fromString(request.conversationId());
        } catch (IllegalArgumentException e) {
            throw ApiException.conversationNotFound();
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(ApiException::conversationNotFound);
        // Videohívás csak kétszemélyes szálban (a kártya 1:1-et ír elő).
        if (conversation.getType() != ConversationType.DIRECT
                || !participantRepository.existsByConversationIdAndUserId(conversationId, senderId)) {
            throw ApiException.conversationNotFound();
        }

        UUID otherId = participantRepository.findUserIdsByConversationId(conversationId).stream()
                .filter(id -> !id.equals(senderId))
                .findFirst()
                .orElseThrow(ApiException::conversationNotFound);

        User sender = userRepository.findById(senderId).orElseThrow(ApiException::userNotFound);
        CallSignal signal = new CallSignal(
                request.conversationId(),
                senderId.toString(),
                sender.getDisplayName(),
                sender.getAvatarUrl(),
                request.type(),
                request.sdp(),
                request.candidate());
        messaging.convertAndSendToUser(otherId.toString(), CALL_DEST, signal);
    }

    /** A {@code RTCPeerConnection}-höz adandó ICE-szerverek a configból (STUN + opcionális TURN). */
    public IceServersDto iceServers() {
        List<IceServersDto.IceServer> servers = new ArrayList<>();
        if (!stunUrls.isEmpty()) {
            servers.add(IceServersDto.IceServer.stun(stunUrls));
        }
        if (!turnUrl.isEmpty()) {
            servers.add(IceServersDto.IceServer.turn(List.of(turnUrl), turnUsername, turnCredential));
        }
        return new IceServersDto(servers);
    }
}
