package com.nexa.realtime;

import com.nexa.follow.FollowRepository;
import com.nexa.friend.FriendshipRepository;
import com.nexa.group.Group;
import com.nexa.group.GroupMemberRepository;
import com.nexa.group.GroupRole;
import com.nexa.post.Post;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A valós idejű értesítés (#11) címzett-feloldásának egységtesztje: pontosan azok kapnak
 * értesítést, akiknek a hírfolyamában megjelenne a poszt — a szerzőt mindig kihagyva.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock SimpMessagingTemplate messaging;
    @Mock FriendshipRepository friendshipRepository;
    @Mock FollowRepository followRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationRepository notificationRepository;

    @InjectMocks NotificationService service;

    private Post profilePost(UUID authorId) {
        User author = mock(User.class);
        when(author.getId()).thenReturn(authorId);
        Post post = mock(Post.class);
        when(post.getAuthor()).thenReturn(author);
        when(post.getGroup()).thenReturn(null);
        return post;
    }

    private Post groupPost(UUID authorId, UUID groupId) {
        User author = mock(User.class);
        when(author.getId()).thenReturn(authorId);
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(groupId);
        Post post = mock(Post.class);
        when(post.getAuthor()).thenReturn(author);
        when(post.getGroup()).thenReturn(group);
        return post;
    }

    @Test
    void profilePost_recipientsAreFriendsAndFollowersWithoutAuthor() {
        UUID author = UUID.randomUUID();
        UUID friend = UUID.randomUUID();
        UUID follower = UUID.randomUUID();
        when(friendshipRepository.findAcceptedFriendIds(author)).thenReturn(List.of(friend));
        when(followRepository.findFollowerIds(author)).thenReturn(List.of(follower, author));

        Set<UUID> recipients = service.resolveRecipients(profilePost(author));

        assertThat(recipients).containsExactlyInAnyOrder(friend, follower);
        assertThat(recipients).doesNotContain(author);
    }

    @Test
    void profilePost_deduplicatesFriendWhoAlsoFollows() {
        UUID author = UUID.randomUUID();
        UUID both = UUID.randomUUID();
        when(friendshipRepository.findAcceptedFriendIds(author)).thenReturn(List.of(both));
        when(followRepository.findFollowerIds(author)).thenReturn(List.of(both));

        Set<UUID> recipients = service.resolveRecipients(profilePost(author));

        assertThat(recipients).containsExactly(both);
    }

    @Test
    void groupPost_recipientsAreGroupMembersWithoutAuthor() {
        UUID author = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        // Csoport-posztnál a baráti/követői repókat nem szabad megkérdezni.
        lenient().when(friendshipRepository.findAcceptedFriendIds(author)).thenReturn(List.of());
        when(groupMemberRepository.findUserIdsByGroupId(groupId))
                .thenReturn(List.of(author, member));

        Set<UUID> recipients = service.resolveRecipients(groupPost(author, groupId));

        assertThat(recipients).containsExactly(member);
    }

    private User user(UUID id, String name) {
        User u = mock(User.class);
        lenient().when(u.getId()).thenReturn(id);
        lenient().when(u.getDisplayName()).thenReturn(name);
        return u;
    }

    private Notification savedNotificationStub() {
        Notification saved = mock(Notification.class);
        when(saved.getId()).thenReturn(UUID.randomUUID());
        lenient().when(saved.getType()).thenReturn(NotificationType.GROUP_JOIN_REQUEST);
        lenient().when(saved.getActorId()).thenReturn(UUID.randomUUID());
        lenient().when(saved.getCreatedAt()).thenReturn(java.time.Instant.now());
        return saved;
    }

    @Test
    void groupJoinRequest_notifiesAdminButNotTheRequester() {
        UUID requesterId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User requester = user(requesterId, "Bob");
        Group group = mock(Group.class);
        UUID groupId = UUID.randomUUID();
        when(group.getId()).thenReturn(groupId);
        when(group.getName()).thenReturn("Privát klub");

        User admin = user(adminId, "Ann");
        Notification saved = savedNotificationStub();
        when(groupMemberRepository.findUserIdsByGroupIdAndRole(groupId, GroupRole.ADMIN))
                .thenReturn(List.of(adminId));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(notificationRepository.save(any())).thenReturn(saved);

        service.notifyGroupJoinRequest(requester, group);

        verify(notificationRepository).save(any());
        verify(messaging).convertAndSendToUser(eq(adminId.toString()), eq("/queue/notifications"), any());
    }

    @Test
    void groupJoinRequest_adminWithPreferenceOffIsSkipped() {
        UUID requesterId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        User requester = user(requesterId, "Bob");
        User admin = user(adminId, "Ann");
        lenient().when(admin.getNotificationPrefs()).thenReturn(
                new com.nexa.user.NotificationPrefs(true, true, true, true, false));
        Group group = mock(Group.class);
        UUID groupId = UUID.randomUUID();
        when(group.getId()).thenReturn(groupId);

        when(groupMemberRepository.findUserIdsByGroupIdAndRole(groupId, GroupRole.ADMIN))
                .thenReturn(List.of(adminId));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        service.notifyGroupJoinRequest(requester, group);

        verify(notificationRepository, never()).save(any());
        verify(messaging, never()).convertAndSendToUser(any(), eq("/queue/notifications"), any());
    }

    @Test
    void groupJoinRequest_adminRequestingOwnGroupDoesNotNotifyThemselves() {
        UUID adminId = UUID.randomUUID();
        User admin = user(adminId, "Ann");
        Group group = mock(Group.class);
        UUID groupId = UUID.randomUUID();
        when(group.getId()).thenReturn(groupId);

        when(groupMemberRepository.findUserIdsByGroupIdAndRole(groupId, GroupRole.ADMIN))
                .thenReturn(List.of(adminId));

        service.notifyGroupJoinRequest(admin, group);

        verify(notificationRepository, never()).save(any());
        verify(messaging, never()).convertAndSendToUser(any(), eq("/queue/notifications"), any());
    }
}
