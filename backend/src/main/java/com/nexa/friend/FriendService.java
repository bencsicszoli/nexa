package com.nexa.friend;

import com.nexa.common.ApiException;
import com.nexa.friend.dto.FriendDto;
import com.nexa.friend.dto.FriendRequestDto;
import com.nexa.friend.dto.FriendRequestsDto;
import com.nexa.friend.dto.UserSummaryDto;
import com.nexa.realtime.NotificationService;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ismerősi kapcsolatok üzleti logikája (#7): kérés küldése, elfogadása, elutasítása /
 * visszavonása, ismerős eltávolítása, valamint a böngészéshez tartozó kapcsolatállapot.
 * <p>
 * Egy pár között legfeljebb egy {@link Friendship} rekord él; a fordított irányú duplikált
 * kérést szándékosan nem engedjük (a meglévő kérést kell elfogadni). Elutasítás / visszavonás
 * a rekordot törli, így később újra lehet próbálkozni.
 */
@Service
public class FriendService {

    /** Az „Emberek" böngészés egy lapjának felső korlátja (a teljes kereső a #16). */
    private static final int BROWSE_LIMIT = 50;

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FriendService(FriendshipRepository friendshipRepository, UserRepository userRepository,
                         NotificationService notificationService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /** Ismerőskérés küldése egy másik felhasználónak. */
    @Transactional
    public void sendRequest(UUID requesterId, UUID targetId) {
        if (requesterId.equals(targetId)) {
            throw ApiException.selfFriendRequest();
        }
        User target = userRepository.findById(targetId).orElseThrow(ApiException::userNotFound);
        User requester = userRepository.findById(requesterId).orElseThrow(ApiException::userNotFound);

        friendshipRepository.findBetween(requesterId, targetId).ifPresent(existing -> {
            if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                throw ApiException.alreadyFriends();
            }
            // Függőben lévő kérés: ki kezdeményezte?
            if (existing.getRequester().getId().equals(requesterId)) {
                throw ApiException.friendRequestAlreadySent();
            }
            throw ApiException.reverseFriendRequestExists();
        });

        friendshipRepository.save(new Friendship(requester, target));
        // Valós idejű + perzisztált értesítés a címzettnek (#17).
        notificationService.notifyFriendRequest(requester, targetId);
    }

    /** Egy beérkezett kérés elfogadása — csak a címzett teheti. */
    @Transactional
    public void acceptRequest(UUID userId, UUID requestId) {
        Friendship request = loadIncomingPending(userId, requestId);
        request.accept(Instant.now());
        // Az eredeti kérelmezőt értesítjük; az aktor az elfogadó (a címzett) (#17).
        notificationService.notifyFriendAccepted(request.getAddressee(), request.getRequester().getId());
    }

    /**
     * Egy függőben lévő kérés megszüntetése: a címzett elutasíthatja, a kezdeményező
     * visszavonhatja. Mindkét esetben törli a rekordot.
     */
    @Transactional
    public void removeRequest(UUID userId, UUID requestId) {
        Friendship request = friendshipRepository.findById(requestId)
                .orElseThrow(ApiException::friendRequestNotFound);
        boolean involved = request.getRequester().getId().equals(userId)
                || request.getAddressee().getId().equals(userId);
        if (request.getStatus() != FriendshipStatus.PENDING || !involved) {
            throw ApiException.friendRequestNotFound();
        }
        friendshipRepository.delete(request);
    }

    /** Ismerős eltávolítása (a kapcsolat törlése, irányától függetlenül). */
    @Transactional
    public void removeFriend(UUID userId, UUID otherUserId) {
        Friendship friendship = friendshipRepository.findBetween(userId, otherUserId)
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElseThrow(ApiException::notFriends);
        friendshipRepository.delete(friendship);
    }

    /**
     * A hívó és egy másik felhasználó közötti kapcsolatállapot a publikus profil
     * kapcsolat-gombjához. A {@code status} ugyanazt a sémát követi, mint a böngészés
     * ({@code NONE} / {@code FRIENDS} / {@code REQUEST_SENT} / {@code REQUEST_RECEIVED});
     * a {@code requestId} csak függő kérésnél van kitöltve (elfogadáshoz / visszavonáshoz).
     * A hívót önmagával nem hívjuk ezzel (azt a {@code UserService} külön kezeli).
     */
    @Transactional(readOnly = true)
    public RelationshipView relationshipWith(UUID viewerId, UUID targetId) {
        return friendshipRepository.findBetween(viewerId, targetId)
                .map(rel -> {
                    if (rel.getStatus() == FriendshipStatus.ACCEPTED) {
                        return new RelationshipView("FRIENDS", null);
                    }
                    String status = rel.getRequester().getId().equals(viewerId)
                            ? "REQUEST_SENT" : "REQUEST_RECEIVED";
                    return new RelationshipView(status, rel.getId().toString());
                })
                .orElse(new RelationshipView("NONE", null));
    }

    /** A hívóhoz viszonyított ismerősi állapot (státusz + a függő kérés azonosítója). */
    public record RelationshipView(String status, String requestId) {
    }

    /** A bejelentkezett felhasználó elfogadott ismerősei (legutóbb elfogadott felül). */
    @Transactional(readOnly = true)
    public List<FriendDto> listFriends(UUID userId) {
        return friendshipRepository.findAcceptedForUser(userId).stream()
                .map(f -> FriendDto.of(other(f, userId), f.getRespondedAt()))
                .toList();
    }

    /** A függőben lévő kérések mindkét iránya (beérkezett + elküldött). */
    @Transactional(readOnly = true)
    public FriendRequestsDto listRequests(UUID userId) {
        List<FriendRequestDto> incoming = friendshipRepository
                .findByAddresseeIdAndStatusOrderByCreatedAtDesc(userId, FriendshipStatus.PENDING).stream()
                .map(f -> FriendRequestDto.of(f.getId().toString(), f.getRequester(), f.getCreatedAt()))
                .toList();
        List<FriendRequestDto> outgoing = friendshipRepository
                .findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, FriendshipStatus.PENDING).stream()
                .map(f -> FriendRequestDto.of(f.getId().toString(), f.getAddressee(), f.getCreatedAt()))
                .toList();
        return new FriendRequestsDto(incoming, outgoing);
    }

    /**
     * Felhasználók böngészése a köztük és a hívó közötti kapcsolatállapottal együtt, hogy a
     * UI a megfelelő műveletet ajánlhassa fel. Üres szűrő esetén az első {@value #BROWSE_LIMIT}
     * felhasználó (a hívó kizárva).
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDto> browsePeople(UUID userId, String query) {
        String q = query == null ? "" : query.trim();
        List<User> users = userRepository.search(q, userId, PageRequest.of(0, BROWSE_LIMIT));

        // A hívó összes kapcsolata, a másik fél id-jére indexelve — így egy lekérdezésből
        // minden találat állapota kiszámítható (N+1 nélkül).
        Map<UUID, Friendship> byOther = new HashMap<>();
        for (Friendship f : friendshipRepository.findAllForUser(userId)) {
            byOther.put(other(f, userId).getId(), f);
        }

        return users.stream().map(user -> {
            Friendship rel = byOther.get(user.getId());
            if (rel == null) {
                return UserSummaryDto.of(user, "NONE", null);
            }
            if (rel.getStatus() == FriendshipStatus.ACCEPTED) {
                return UserSummaryDto.of(user, "FRIENDS", null);
            }
            String direction = rel.getRequester().getId().equals(userId)
                    ? "REQUEST_SENT" : "REQUEST_RECEIVED";
            return UserSummaryDto.of(user, direction, rel.getId().toString());
        }).toList();
    }

    /** Betölti a hívóhoz beérkezett, függőben lévő kérést (különben 404 — létezést sem szivárogtatunk). */
    private Friendship loadIncomingPending(UUID userId, UUID requestId) {
        Friendship request = friendshipRepository.findById(requestId)
                .orElseThrow(ApiException::friendRequestNotFound);
        if (request.getStatus() != FriendshipStatus.PENDING
                || !request.getAddressee().getId().equals(userId)) {
            throw ApiException.friendRequestNotFound();
        }
        return request;
    }

    /** A kapcsolat „másik fele" a hívó szemszögéből. */
    private User other(Friendship f, UUID userId) {
        return f.getRequester().getId().equals(userId) ? f.getAddressee() : f.getRequester();
    }
}
