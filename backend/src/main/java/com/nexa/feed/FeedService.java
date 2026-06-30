package com.nexa.feed;

import com.nexa.common.ApiException;
import com.nexa.feed.dto.FeedPageDto;
import com.nexa.follow.FollowRepository;
import com.nexa.friend.FriendshipRepository;
import com.nexa.group.GroupMemberRepository;
import com.nexa.post.Post;
import com.nexa.post.PostRepository;
import com.nexa.post.dto.PostDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Az algoritmusmentes, időrendi hírfolyam összeállítása (#10). A folyam KIZÁRÓLAG a
 * felhasználó <b>ismerőseinek</b> és <b>követettjeinek</b> profil-posztjait, valamint a
 * <b>tag-csoportjai</b> bejegyzéseit tartalmazza — legfrissebb felül. Nincs benne ajánló
 * vagy rangsoroló logika; a sorrend tisztán az utolsó aktivitás {@code (lastActivityAt, id)}
 * pár szerinti — egy bejegyzés a létrehozásakor, majd minden új hozzászóláskor a tetejére kerül.
 * <p>
 * A lapozás cursor-alapú (keyset): minden lap egy átlátszatlan {@code nextCursor}-t ad,
 * amely az utolsó bejegyzés {@code (aktivitás, id)} párját kódolja. Ez offset-mentes, így
 * új poszt/aktivitás érkezésekor sem hagy ki és nem ismétel bejegyzést.
 */
@Service
public class FeedService {

    /** A lapméret felső korlátja; a kérésben adott nagyobb értéket erre vágjuk. */
    public static final int MAX_LIMIT = 50;
    /** A lapméret alapértéke, ha a kérés nem ad meg határt. */
    public static final int DEFAULT_LIMIT = 20;

    /**
     * Üres szerző-/csoporthalmaznál használt helyőrző, hogy a JPQL {@code in (...)} sose
     * legyen üres. A {@code GeneratedValue} v4 UUID-k sosem egyeznek a nil-UUID-vel.
     */
    private static final UUID NIL = new UUID(0L, 0L);

    private final PostRepository postRepository;
    private final FriendshipRepository friendshipRepository;
    private final FollowRepository followRepository;
    private final GroupMemberRepository groupMemberRepository;

    public FeedService(PostRepository postRepository,
                       FriendshipRepository friendshipRepository,
                       FollowRepository followRepository,
                       GroupMemberRepository groupMemberRepository) {
        this.postRepository = postRepository;
        this.friendshipRepository = friendshipRepository;
        this.followRepository = followRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    /**
     * A felhasználó hírfolyamának egy lapja. {@code cursor} null/üres az első lapnál; egyébként
     * egy korábbi lap {@code nextCursor}-a. A {@code limit} 1 és {@value #MAX_LIMIT} közé vágva.
     */
    @Transactional(readOnly = true)
    public FeedPageDto getFeed(UUID userId, String cursor, int limit) {
        int pageSize = Math.max(1, Math.min(limit, MAX_LIMIT));

        // Az aggregált forráshalmaz: ismerősök + követettek profil-posztjai, és a tag-csoportok.
        // A saját profil-posztok szándékosan kimaradnak (a felhasználó nem ismerőse/követettje
        // önmagának); a saját csoport-posztok viszont a tag-csoport részeként megjelennek.
        Set<UUID> authorIds = new LinkedHashSet<>(friendshipRepository.findAcceptedFriendIds(userId));
        authorIds.addAll(followRepository.findFolloweeIds(userId));
        List<UUID> groupIds = groupMemberRepository.findGroupIdsByUserId(userId);

        Collection<UUID> authorParam = authorIds.isEmpty() ? List.of(NIL) : authorIds;
        Collection<UUID> groupParam = groupIds.isEmpty() ? List.of(NIL) : groupIds;

        // Egy extra sort kérünk (pageSize + 1): ha visszajön, van még következő lap.
        Pageable page = PageRequest.of(0, pageSize + 1);
        List<Post> posts;
        if (cursor == null || cursor.isBlank()) {
            posts = postRepository.findFeedFirstPage(authorParam, groupParam, page);
        } else {
            Cursor c = decodeCursor(cursor);
            posts = postRepository.findFeedAfter(authorParam, groupParam, c.activityAt(), c.id(), page);
        }

        boolean hasMore = posts.size() > pageSize;
        List<Post> pagePosts = hasMore ? posts.subList(0, pageSize) : posts;

        List<PostDto> items = pagePosts.stream().map(PostDto::from).toList();
        String nextCursor = hasMore ? encodeCursor(pagePosts.get(pagePosts.size() - 1)) : null;
        return new FeedPageDto(items, nextCursor);
    }

    /** A lapozási cursor dekódolt tartalma: az utolsó bejegyzés rendezési kulcsa (aktivitás + id). */
    private record Cursor(Instant activityAt, UUID id) {
    }

    /** Egy bejegyzés {@code (aktivitás, id)} párját átlátszatlan, URL-biztos cursorrá kódolja. */
    private String encodeCursor(Post post) {
        String raw = post.getLastActivityAt().toString() + "|" + post.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** A cursor visszafejtése; sérült/idegen érték esetén {@code INVALID_CURSOR}. */
    private Cursor decodeCursor(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.lastIndexOf('|');
            if (sep < 0) {
                throw new IllegalArgumentException("missing separator");
            }
            Instant activityAt = Instant.parse(raw.substring(0, sep));
            UUID id = UUID.fromString(raw.substring(sep + 1));
            return new Cursor(activityAt, id);
        } catch (RuntimeException e) {
            throw ApiException.invalidCursor();
        }
    }
}
