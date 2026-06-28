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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A {@link CallService} egységtesztje: a videohívás-jelzést a {@code DIRECT} szál másik
 * résztvevőjének relézzük, idegen/csoport-szál esetén elutasítjuk, és az ICE-szerver lista a
 * configból épül föl.
 */
class CallServiceTest {

    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final ConversationParticipantRepository participantRepository =
            mock(ConversationParticipantRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);

    private CallService service(String stun, String turnUrl, String turnUser, String turnCred) {
        return new CallService(conversationRepository, participantRepository, userRepository,
                messaging, stun, turnUrl, turnUser, turnCred);
    }

    private CallService service() {
        return service("stun:stun.example:3478", "", "", "");
    }

    @Test
    void relay_forwardsSignalToOtherParticipant() {
        UUID sender = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        Conversation conversation = mock(Conversation.class);
        when(conversation.getType()).thenReturn(ConversationType.DIRECT);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(participantRepository.existsByConversationIdAndUserId(conversationId, sender)).thenReturn(true);
        when(participantRepository.findUserIdsByConversationId(conversationId))
                .thenReturn(List.of(sender, other));
        User senderUser = mock(User.class);
        when(senderUser.getDisplayName()).thenReturn("Anna");
        when(senderUser.getAvatarUrl()).thenReturn("http://x/a.png");
        when(userRepository.findById(sender)).thenReturn(Optional.of(senderUser));

        service().relay(sender, new CallSignalRequest(conversationId.toString(),
                CallSignalType.OFFER, null, null));

        ArgumentCaptor<CallSignal> captor = ArgumentCaptor.forClass(CallSignal.class);
        verify(messaging).convertAndSendToUser(eq(other.toString()), eq("/queue/call"), captor.capture());
        CallSignal sent = captor.getValue();
        assertThat(sent.type()).isEqualTo(CallSignalType.OFFER);
        assertThat(sent.fromUserId()).isEqualTo(sender.toString());
        assertThat(sent.fromName()).isEqualTo("Anna");
        assertThat(sent.fromAvatarUrl()).isEqualTo("http://x/a.png");
    }

    @Test
    void relay_nonParticipant_isRejected() {
        UUID stranger = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = mock(Conversation.class);
        when(conversation.getType()).thenReturn(ConversationType.DIRECT);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(participantRepository.existsByConversationIdAndUserId(conversationId, stranger)).thenReturn(false);

        assertThatThrownBy(() -> service().relay(stranger,
                new CallSignalRequest(conversationId.toString(), CallSignalType.OFFER, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("CONVERSATION_NOT_FOUND");
        verify(messaging, never()).convertAndSendToUser(eq(stranger.toString()), eq("/queue/call"), eq(null));
    }

    @Test
    void relay_groupConversation_isRejected() {
        UUID sender = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = mock(Conversation.class);
        when(conversation.getType()).thenReturn(ConversationType.GROUP);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> service().relay(sender,
                new CallSignalRequest(conversationId.toString(), CallSignalType.OFFER, null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("CONVERSATION_NOT_FOUND");
    }

    @Test
    void iceServers_includesStunAndTurnFromConfig() {
        IceServersDto dto = service("stun:a:3478,stun:b:3478",
                "turn:turn.example:3478", "user", "secret").iceServers();

        assertThat(dto.iceServers()).hasSize(2);
        IceServersDto.IceServer stun = dto.iceServers().get(0);
        assertThat(stun.urls()).containsExactly("stun:a:3478", "stun:b:3478");
        assertThat(stun.username()).isNull();
        IceServersDto.IceServer turn = dto.iceServers().get(1);
        assertThat(turn.urls()).containsExactly("turn:turn.example:3478");
        assertThat(turn.username()).isEqualTo("user");
        assertThat(turn.credential()).isEqualTo("secret");
    }

    @Test
    void iceServers_withoutTurn_hasOnlyStun() {
        IceServersDto dto = service().iceServers();
        assertThat(dto.iceServers()).hasSize(1);
        assertThat(dto.iceServers().get(0).username()).isNull();
    }
}
