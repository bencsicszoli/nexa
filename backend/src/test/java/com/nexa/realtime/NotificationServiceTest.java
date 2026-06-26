package com.nexa.realtime;

import com.nexa.follow.FollowRepository;
import com.nexa.friend.FriendshipRepository;
import com.nexa.group.Group;
import com.nexa.group.GroupMemberRepository;
import com.nexa.post.Post;
import com.nexa.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
}
