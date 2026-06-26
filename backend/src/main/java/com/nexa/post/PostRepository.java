package com.nexa.post;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    /**
     * Egy szerző profil-bejegyzései időrendben, legfrissebb felül (profil-időrend).
     * A csoportba írt posztok ({@code group} nem null) szándékosan kimaradnak — azok
     * a csoportoldalon jelennek meg, nem a szerző profilján.
     */
    List<Post> findByAuthorIdAndGroupIsNullOrderByCreatedAtDesc(UUID authorId);

    /** Egy csoport bejegyzései időrendben, legfrissebb felül (csoport-időrend, #9). */
    List<Post> findByGroupIdOrderByCreatedAtDesc(UUID groupId);

    /**
     * A hírfolyam (#10) első oldala: az ismerősök + követettek <b>profil</b>-posztjai
     * ({@code group is null}), valamint a tag-csoportok posztjai — időrendben, legfrissebb felül.
     * A sorrend stabil rendezése a {@code (createdAt, id)} páron megy, hogy a cursor-lapozás
     * (lásd {@link #findFeedAfter}) ne hagyjon ki és ne ismételjen sort azonos időbélyegnél.
     * A lapméretet a {@code pageable} adja. A két id-halmaz sosem üres (a hívó nil-UUID-vel tölti).
     */
    @Query("""
            select p from Post p
            where (p.group is null and p.author.id in :authorIds)
               or (p.group is not null and p.group.id in :groupIds)
            order by p.createdAt desc, p.id desc
            """)
    List<Post> findFeedFirstPage(@Param("authorIds") Collection<UUID> authorIds,
                                 @Param("groupIds") Collection<UUID> groupIds,
                                 Pageable pageable);

    /**
     * A hírfolyam következő oldala a {@code (beforeCreatedAt, beforeId)} cursornál régebbi
     * posztoktól — keyset-lapozás, ugyanazzal a {@code (createdAt, id)} rendezéssel, mint
     * {@link #findFeedFirstPage}. Az offset-lapozással szemben ez akkor sem hagy ki posztot,
     * ha közben új bejegyzés érkezik a folyam tetejére.
     */
    @Query("""
            select p from Post p
            where ((p.group is null and p.author.id in :authorIds)
                or (p.group is not null and p.group.id in :groupIds))
              and (p.createdAt < :beforeCreatedAt
                or (p.createdAt = :beforeCreatedAt and p.id < :beforeId))
            order by p.createdAt desc, p.id desc
            """)
    List<Post> findFeedAfter(@Param("authorIds") Collection<UUID> authorIds,
                             @Param("groupIds") Collection<UUID> groupIds,
                             @Param("beforeCreatedAt") Instant beforeCreatedAt,
                             @Param("beforeId") UUID beforeId,
                             Pageable pageable);
}
