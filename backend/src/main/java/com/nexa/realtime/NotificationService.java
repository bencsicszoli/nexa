package com.nexa.realtime;

import com.nexa.follow.FollowRepository;
import com.nexa.friend.FriendshipRepository;
import com.nexa.group.Group;
import com.nexa.group.GroupMemberRepository;
import com.nexa.post.Post;
import com.nexa.realtime.dto.NotificationDto;
import com.nexa.user.User;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Valós idejű értesítés új tartalomról (#11). Új bejegyzés létrehozásakor azokat a
 * felhasználókat értesíti, akiknek a {@link com.nexa.feed.FeedService hírfolyamában}
 * megjelenne a poszt:
 * <ul>
 *   <li><b>profil-poszt:</b> a szerző ismerősei (kétirányú) + követői,</li>
 *   <li><b>csoport-poszt:</b> a csoport tagjai,</li>
 * </ul>
 * a szerzőt magát mindig kihagyva (a saját poszt nincs a saját folyamban).
 *
 * <p>Az értesítés a DB-tranzakció <b>commitja után</b> megy ki: így a címzett a kattintásra
 * induló hírfolyam-lekérdezésben már biztosan látja az új posztot, rollbacknál pedig nem
 * küldünk értesítést nemlétező tartalomról. A push best-effort (in-memory STOMP bróker).
 */
@Service
public class NotificationService {

    private static final String DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messaging;
    private final FriendshipRepository friendshipRepository;
    private final FollowRepository followRepository;
    private final GroupMemberRepository groupMemberRepository;

    public NotificationService(SimpMessagingTemplate messaging,
                               FriendshipRepository friendshipRepository,
                               FollowRepository followRepository,
                               GroupMemberRepository groupMemberRepository) {
        this.messaging = messaging;
        this.friendshipRepository = friendshipRepository;
        this.followRepository = followRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    /**
     * Új bejegyzésről értesíti a címzetteket. A hívás egy aktív tranzakción belül várt
     * (a poszt mentése után); a tényleges push a commit után fut.
     */
    public void notifyNewPost(Post post) {
        Set<UUID> recipients = resolveRecipients(post);
        if (recipients.isEmpty()) {
            return;
        }
        NotificationDto dto = toNotification(post);
        publishAfterCommit(recipients, dto);
    }

    /**
     * Az értesítés címzettjei (a szerző nélkül). Csomag-láthatóságban tesztelhető.
     */
    Set<UUID> resolveRecipients(Post post) {
        UUID authorId = post.getAuthor().getId();
        Set<UUID> recipients = new LinkedHashSet<>();
        if (post.getGroup() == null) {
            recipients.addAll(friendshipRepository.findAcceptedFriendIds(authorId));
            recipients.addAll(followRepository.findFollowerIds(authorId));
        } else {
            recipients.addAll(groupMemberRepository.findUserIdsByGroupId(post.getGroup().getId()));
        }
        recipients.remove(authorId);
        return recipients;
    }

    private NotificationDto toNotification(Post post) {
        User author = post.getAuthor();
        Group group = post.getGroup();
        return new NotificationDto(
                UUID.randomUUID().toString(),
                NotificationDto.TYPE_NEW_POST,
                post.getId().toString(),
                author.getId().toString(),
                author.getDisplayName(),
                author.getAvatarUrl(),
                group == null ? null : group.getId().toString(),
                group == null ? null : group.getName(),
                group == null ? null : group.getLogoUrl(),
                post.getCreatedAt());
    }

    /** A push ütemezése a commit utánra; aktív tranzakció hiányában azonnal megy (pl. teszt). */
    private void publishAfterCommit(Set<UUID> recipients, NotificationDto dto) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(recipients, dto);
                }
            });
        } else {
            send(recipients, dto);
        }
    }

    private void send(Set<UUID> recipients, NotificationDto dto) {
        for (UUID recipient : recipients) {
            messaging.convertAndSendToUser(recipient.toString(), DESTINATION, dto);
        }
    }
}
