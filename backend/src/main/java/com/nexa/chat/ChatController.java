package com.nexa.chat;

import com.nexa.chat.dto.ChatMessageDto;
import com.nexa.chat.dto.ConversationDto;
import com.nexa.chat.dto.MessagesPageDto;
import com.nexa.chat.dto.PostMessageRequest;
import com.nexa.chat.dto.StartDirectRequest;
import com.nexa.subscription.SubscriptionRequired;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Csevegés-végpontok az {@code /api/chat} prefix alatt — mind hitelesítést igényel (#12).
 * A beszélgetéslista, a szálnyitás, az időrendi előzmény és az olvasottság REST-en; az élő
 * üzenetküldés/gépelés a STOMP {@link ChatMessageController}-en megy (a {@code POST .../messages}
 * megbízható tartalék, pl. ha a WebSocket épp nem elérhető).
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** A bejelentkezett felhasználó beszélgetései, legutóbb aktív felül. */
    @GetMapping("/conversations")
    public List<ConversationDto> conversations(@AuthenticationPrincipal UUID userId) {
        return chatService.listConversations(userId);
    }

    /** Kétszemélyes szál megnyitása/létrehozása egy másik felhasználóval. */
    @PostMapping("/conversations/direct")
    @SubscriptionRequired
    public ConversationDto startDirect(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody StartDirectRequest request) {
        return chatService.startDirect(userId, request.userId());
    }

    /** Egy csoport csevegő-szálának megnyitása/létrehozása (csak tagnak). */
    @PostMapping("/conversations/group/{groupId}")
    @SubscriptionRequired
    public ConversationDto openGroup(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID groupId) {
        return chatService.openGroup(userId, groupId);
    }

    /** Egy szál üzenet-előzménye (időrendben), és a szál friss metaadata. Olvasottá teszi a szálat. */
    @GetMapping("/conversations/{id}/messages")
    public MessagesPageDto messages(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "30") int limit) {
        return chatService.messages(userId, id, cursor, limit);
    }

    /** Üzenetküldés REST-en (megbízható tartalék a STOMP mellé). */
    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @SubscriptionRequired
    public ChatMessageDto send(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody PostMessageRequest request) {
        return chatService.sendMessage(userId, id, request.content());
    }

    /** A szál olvasottá jelölése (az olvasatlan-jelvény nullázásához). */
    @PostMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        chatService.markRead(userId, id);
    }
}
