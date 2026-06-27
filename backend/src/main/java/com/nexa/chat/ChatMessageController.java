package com.nexa.chat;

import com.nexa.chat.dto.SendMessageRequest;
import com.nexa.chat.dto.TypingRequest;
import com.nexa.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * A csevegés élő, STOMP-os bemenete (#12). A kliens a {@code /app/chat.send} és a
 * {@code /app/chat.typing} célokra küld; a hitelesített felhasználót a STOMP CONNECT-kor
 * beállított {@link StompPrincipal} adja ({@code principal.getName()} = userId).
 *
 * <p>A küldés üzleti hibáit (pl. nincs hozzáférés, üres szöveg) itt elnyeljük: egy hibás
 * üzenettől nem szakítjuk meg a kliens WebSocket-kapcsolatát — a felhasználó-felé
 * visszaigazolás a normál (sikeres) push-on át jön, a megbízható út pedig a REST végpont.
 */
@Controller
public class ChatMessageController {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageController.class);

    private final ChatService chatService;

    public ChatMessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public void send(@Payload SendMessageRequest request, Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        try {
            chatService.sendMessage(userId, request.conversationId(), request.content());
        } catch (ApiException e) {
            log.debug("STOMP üzenetküldés elutasítva ({}): {}", e.getCode(), e.getMessage());
        }
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingRequest request, Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        try {
            chatService.broadcastTyping(userId, request.conversationId());
        } catch (ApiException e) {
            log.debug("STOMP gépelés-jelzés elutasítva ({})", e.getCode());
        }
    }
}
