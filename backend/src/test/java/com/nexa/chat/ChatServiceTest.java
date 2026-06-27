package com.nexa.chat;

import com.nexa.chat.dto.ConversationDto;
import com.nexa.common.ApiException;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A {@link ChatService} szál-kezelésének integrációs tesztje (valós H2 + repók): a kétszemélyes
 * szál idempotens (irányfüggetlenül ugyanaz), és a szálhoz csak a résztvevő fér hozzá.
 */
@SpringBootTest
@Transactional
class ChatServiceTest {

    @Autowired ChatService chatService;
    @Autowired UserRepository userRepository;

    private User newUser(String email, String name) {
        return userRepository.save(new User(email, name, "hash"));
    }

    @Test
    void startDirect_isIdempotentRegardlessOfDirection() {
        User a = newUser("svc-a@example.com", "Anna");
        User b = newUser("svc-b@example.com", "Bela");

        ConversationDto first = chatService.startDirect(a.getId(), b.getId());
        ConversationDto again = chatService.startDirect(a.getId(), b.getId());
        ConversationDto reversed = chatService.startDirect(b.getId(), a.getId());

        assertThat(again.id()).isEqualTo(first.id());
        assertThat(reversed.id()).isEqualTo(first.id());
        // A cím a hívó szemszögéből a másik fél neve.
        assertThat(first.title()).isEqualTo("Bela");
        assertThat(reversed.title()).isEqualTo("Anna");
    }

    @Test
    void startDirect_withSelf_isRejected() {
        User a = newUser("svc-self@example.com", "Anna");
        assertThatThrownBy(() -> chatService.startDirect(a.getId(), a.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("SELF_CONVERSATION");
    }

    @Test
    void nonParticipant_cannotAccessConversation() {
        User a = newUser("svc-x@example.com", "Anna");
        User b = newUser("svc-y@example.com", "Bela");
        User stranger = newUser("svc-z@example.com", "Cili");

        ConversationDto conv = chatService.startDirect(a.getId(), b.getId());
        UUID conversationId = UUID.fromString(conv.id());

        assertThatThrownBy(() -> chatService.messages(stranger.getId(), conversationId, null, 30))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("CONVERSATION_NOT_FOUND");

        assertThatThrownBy(() -> chatService.sendMessage(stranger.getId(), conversationId, "hello"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("CONVERSATION_NOT_FOUND");
    }
}
