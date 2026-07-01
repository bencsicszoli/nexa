package com.nexa.realtime;

import com.nexa.follow.FollowRepository;
import com.nexa.friend.FriendshipRepository;
import com.nexa.group.Group;
import com.nexa.group.GroupMemberRepository;
import com.nexa.group.GroupRole;
import com.nexa.post.Post;
import com.nexa.realtime.dto.NotificationDto;
import com.nexa.common.ApiException;
import com.nexa.realtime.dto.NotificationPageDto;
import com.nexa.user.NotificationPrefs;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Valós idejű ÉS perzisztált értesítés (#11, #17). Minden értesítés előbb a {@code notifications}
 * táblába kerül (a kiváltó tranzakcióban), majd a DB-tranzakció <b>commitja után</b> push-ban is
 * kimegy a címzettnek a STOMP-on (a kattintásra induló lekérdezés így már látja a tartalmat,
 * rollbacknál pedig nem küldünk nemlétezőről értesítést). A push best-effort (in-memory bróker),
 * az előzmény a forrás-igazság (újratöltés után is megmarad).
 *
 * <p><b>Preferencia-szűrés:</b> a címzett {@link NotificationPrefs}-e szerint a kikapcsolt
 * típus sem nem perzisztálódik, sem nem megy ki. {@code NEW_POST}-nál a sok címzett prefjét egy
 * tömeges lekérdezéssel ({@link UserRepository#findAllById}) töltjük be — nincs N+1.
 *
 * <p>Az értesítések fajtái:
 * <ul>
 *   <li><b>NEW_POST:</b> a szerző ismerősei + követői (profil-poszt), vagy a csoport tagjai
 *       (csoport-poszt) — a szerzőt mindig kihagyva,</li>
 *   <li><b>FRIEND_REQUEST / FRIEND_ACCEPTED / NEW_FOLLOWER:</b> egyetlen címzett,</li>
 *   <li><b>GROUP_JOIN_REQUEST:</b> egy privát csoport minden adminja.</li>
 * </ul>
 */
@Service
public class NotificationService {

    private static final String DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messaging;
    private final FriendshipRepository friendshipRepository;
    private final FollowRepository followRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public NotificationService(SimpMessagingTemplate messaging,
                               FriendshipRepository friendshipRepository,
                               FollowRepository followRepository,
                               GroupMemberRepository groupMemberRepository,
                               UserRepository userRepository,
                               NotificationRepository notificationRepository) {
        this.messaging = messaging;
        this.friendshipRepository = friendshipRepository;
        this.followRepository = followRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Új bejegyzésről értesíti a címzetteket (azokat, akik a {@code newPost} prefet nem
     * kapcsolták ki). A hívás egy aktív tranzakción belül várt (a poszt mentése után);
     * a perzisztálás ott, a push a commit után fut.
     */
    public void notifyNewPost(Post post) {
        Set<UUID> recipients = resolveRecipients(post);
        if (recipients.isEmpty()) {
            return;
        }
        // A címzettek prefje egy lekérdezésből (N+1 nélkül), majd csak az engedélyezettek maradnak.
        Map<UUID, NotificationPrefs> prefs = new HashMap<>();
        for (User u : userRepository.findAllById(recipients)) {
            prefs.put(u.getId(), u.getNotificationPrefs());
        }

        User author = post.getAuthor();
        Group group = post.getGroup();
        UUID groupId = group == null ? null : group.getId();
        String groupName = group == null ? null : group.getName();
        String groupLogoUrl = group == null ? null : group.getLogoUrl();

        List<PendingPush> toPush = new ArrayList<>();
        for (UUID recipientId : recipients) {
            NotificationPrefs p = prefs.get(recipientId);
            if (p != null && !p.newPost()) {
                continue;
            }
            Notification saved = notificationRepository.save(Notification.newPost(
                    recipientId, author.getId(), author.getDisplayName(), author.getAvatarUrl(),
                    post.getId(), groupId, groupName, groupLogoUrl));
            toPush.add(new PendingPush(recipientId, NotificationDto.from(saved)));
        }
        pushAfterCommit(toPush);
    }

    /** Ismerőskérés a címzettnek (az aktor a kérelmező). */
    public void notifyFriendRequest(User actor, UUID recipientId) {
        relationshipNotification(NotificationType.FRIEND_REQUEST, actor, recipientId,
                NotificationPrefs::friendRequest);
    }

    /** Elfogadott ismerőskérés a címzettnek (az aktor az elfogadó). */
    public void notifyFriendAccepted(User actor, UUID recipientId) {
        relationshipNotification(NotificationType.FRIEND_ACCEPTED, actor, recipientId,
                NotificationPrefs::friendAccepted);
    }

    /** Új követő a címzettnek (az aktor a követő). */
    public void notifyNewFollower(User actor, UUID recipientId) {
        relationshipNotification(NotificationType.NEW_FOLLOWER, actor, recipientId,
                NotificationPrefs::newFollower);
    }

    /** Csoport-csatlakozási kérelem a csoport minden adminjának (az aktor a kérelmező). */
    public void notifyGroupJoinRequest(User actor, Group group) {
        List<UUID> adminIds = groupMemberRepository.findUserIdsByGroupIdAndRole(
                group.getId(), GroupRole.ADMIN);
        List<PendingPush> toPush = new ArrayList<>();
        for (UUID adminId : adminIds) {
            if (adminId.equals(actor.getId())) {
                continue;
            }
            NotificationPrefs prefs = userRepository.findById(adminId)
                    .map(User::getNotificationPrefs)
                    .orElse(null);
            if (prefs != null && !prefs.groupJoinRequest()) {
                continue;
            }
            Notification saved = notificationRepository.save(Notification.groupJoinRequest(
                    adminId, actor.getId(), actor.getDisplayName(), actor.getAvatarUrl(),
                    group.getId(), group.getName(), group.getLogoUrl()));
            toPush.add(new PendingPush(adminId, NotificationDto.from(saved)));
        }
        pushAfterCommit(toPush);
    }

    /** Egy kapcsolati értesítés perzisztálása + push, ha a címzett a típust nem kapcsolta ki. */
    private void relationshipNotification(NotificationType type, User actor, UUID recipientId,
                                          Predicate<NotificationPrefs> allowed) {
        if (recipientId.equals(actor.getId())) {
            return;
        }
        NotificationPrefs prefs = userRepository.findById(recipientId)
                .map(User::getNotificationPrefs)
                .orElse(null);
        if (prefs != null && !allowed.test(prefs)) {
            return;
        }
        Notification saved = notificationRepository.save(Notification.relationship(
                recipientId, type, actor.getId(), actor.getDisplayName(), actor.getAvatarUrl()));
        pushAfterCommit(List.of(new PendingPush(recipientId, NotificationDto.from(saved))));
    }

    // --- Előzmény / olvasottság (#17) ---

    /** Az alap- és maximális lapméret az előzmény-lekérdezéshez. */
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;

    /** Egy felhasználó értesítés-előzményének lapja, legfrissebb felül. */
    @Transactional(readOnly = true)
    public NotificationPageDto history(UUID userId, int page, int size) {
        int p = Math.max(0, page);
        int pageSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        // Egy elemmel többet kérünk, hogy tudjuk: van-e még régebbi lap.
        var rows = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                userId, PageRequest.of(p, pageSize + 1));
        boolean hasMore = rows.size() > pageSize;
        List<NotificationDto> items = rows.stream()
                .limit(pageSize)
                .map(NotificationDto::from)
                .toList();
        return new NotificationPageDto(items, p, hasMore);
    }

    /** Az olvasatlan értesítések száma (harang-jelvény). */
    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    /** Minden értesítés olvasottra állítása. */
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId);
    }

    /** Egy értesítés olvasottra állítása (csak a sajátját; idegen id → 404). */
    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(ApiException::notificationNotFound);
        n.markRead();
    }

    /**
     * Az értesítés címzettjei egy poszthoz (a szerző nélkül). Csomag-láthatóságban tesztelhető.
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

    /** A push-ok ütemezése a commit utánra; aktív tranzakció hiányában azonnal megy (pl. teszt). */
    private void pushAfterCommit(List<PendingPush> pushes) {
        if (pushes.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(pushes);
                }
            });
        } else {
            send(pushes);
        }
    }

    private void send(List<PendingPush> pushes) {
        for (PendingPush push : pushes) {
            messaging.convertAndSendToUser(push.recipientId().toString(), DESTINATION, push.dto());
        }
    }

    /** Egy kiküldésre váró értesítés a címzett id-jával (a DTO maga nem hordozza a címzettet). */
    private record PendingPush(UUID recipientId, NotificationDto dto) {
    }
}
