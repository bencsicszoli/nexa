package com.nexa.post;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Egy szerző saját profil-bejegyzései a hírfolyaméhoz (#10) hasonló rendezéssel: a legutóbbi
     * <b>aktivitás</b> ({@code coalesce(lastActivityAt, createdAt)}) felül — így egy régebbi
     * bejegyzésre érkező friss hozzászólás a saját profil tetejére hozza a posztot. A rendezés a
     * {@code (aktivitás, id)} páron stabil. (A csoport-posztok kimaradnak, mint a profil-időrendnél.)
     */
    @Query("""
            select p from Post p
            where p.author.id = :authorId and p.group is null
            order by coalesce(p.lastActivityAt, p.createdAt) desc, p.id desc
            """)
    List<Post> findOwnProfilePostsByActivity(@Param("authorId") UUID authorId);

    /** Egy csoport bejegyzései időrendben, legfrissebb felül (csoport-időrend, #9). */
    List<Post> findByGroupIdOrderByCreatedAtDesc(UUID groupId);

    /**
     * A hírfolyam (#10) első oldala: az ismerősök + követettek <b>profil</b>-posztjai
     * ({@code group is null}), valamint a tag-csoportok posztjai — időrendben, legfrissebb
     * <b>aktivitás</b> felül. A rendezés kulcsa az utolsó aktivitás
     * ({@code coalesce(lastActivityAt, createdAt)}): új hozzászólás a bejegyzést a tetejére tolja.
     * A stabil rendezés a {@code (aktivitás, id)} páron megy, hogy a cursor-lapozás (lásd
     * {@link #findFeedAfter}) ne hagyjon ki és ne ismételjen sort azonos időbélyegnél.
     * A lapméretet a {@code pageable} adja. A két id-halmaz sosem üres (a hívó nil-UUID-vel tölti).
     */
    @Query("""
            select p from Post p
            where (p.group is null and p.author.id in :authorIds)
               or (p.group is not null and p.group.id in :groupIds)
            order by coalesce(p.lastActivityAt, p.createdAt) desc, p.id desc
            """)
    List<Post> findFeedFirstPage(@Param("authorIds") Collection<UUID> authorIds,
                                 @Param("groupIds") Collection<UUID> groupIds,
                                 Pageable pageable);

    /**
     * A hírfolyam következő oldala a {@code (beforeActivityAt, beforeId)} cursornál régebbi
     * (kisebb aktivitású) posztoktól — keyset-lapozás, ugyanazzal a {@code (aktivitás, id)}
     * rendezéssel, mint {@link #findFeedFirstPage}. Az offset-lapozással szemben ez akkor sem
     * hagy ki posztot, ha közben új bejegyzés/aktivitás érkezik a folyam tetejére.
     */
    @Query("""
            select p from Post p
            where ((p.group is null and p.author.id in :authorIds)
                or (p.group is not null and p.group.id in :groupIds))
              and (coalesce(p.lastActivityAt, p.createdAt) < :beforeActivityAt
                or (coalesce(p.lastActivityAt, p.createdAt) = :beforeActivityAt and p.id < :beforeId))
            order by coalesce(p.lastActivityAt, p.createdAt) desc, p.id desc
            """)
    List<Post> findFeedAfter(@Param("authorIds") Collection<UUID> authorIds,
                             @Param("groupIds") Collection<UUID> groupIds,
                             @Param("beforeActivityAt") Instant beforeActivityAt,
                             @Param("beforeId") UUID beforeId,
                             Pageable pageable);

    /**
     * Bejegyzés-keresés (#16) a szövegtöredékre, kis/nagybetűtől függetlenül, legfrissebb felül.
     * Adatvédelmi szűrés: csak olyan poszt jelenhet meg, ami a hívónak amúgy is látható —
     * profil-poszt ({@code group is null}), publikus csoport posztja, vagy a hívó
     * tag-csoportjának posztja. Így privát csoport bejegyzése sosem szivárog ki a keresőben.
     * A {@code myGroupIds} sosem üres (a hívó nil-UUID-vel tölti, mint a hírfolyamnál).
     */
    @Query("""
            select p from Post p left join p.group g
            where lower(p.content) like lower(concat('%', :q, '%'))
              and (g is null
                or g.visibility = :publicVisibility
                or g.id in :myGroupIds)
            order by p.createdAt desc, p.id desc
            """)
    List<Post> search(@Param("q") String q,
                      @Param("myGroupIds") Collection<UUID> myGroupIds,
                      @Param("publicVisibility") com.nexa.group.GroupVisibility publicVisibility,
                      Pageable pageable);

    /**
     * Egyszeri visszatöltés (a {@code last_activity_at} bevezetése előtti sorokhoz): a már
     * hozzászólást kapott bejegyzéseknél az aktivitást a legfrissebb komment idejére állítja.
     * Csak a {@code null} (még sosem bump-olt = régi) sorokat érinti, így a fix utáni
     * aktivitásokat nem írja felül. Hordozható natív SQL (H2 + PostgreSQL).
     */
    @Modifying
    @Query(value = """
            update posts p set last_activity_at = (
                select max(c.created_at) from comments c where c.post_id = p.id)
            where p.last_activity_at is null
              and exists (select 1 from comments c where c.post_id = p.id)
            """, nativeQuery = true)
    int backfillActivityFromComments();

    /**
     * A maradék {@code null} (hozzászólás nélküli, régi) sor aktivitását a létrehozás idejére
     * állítja, hogy az oszlop önkonzisztens legyen. A {@link #backfillActivityFromComments} után fut.
     */
    @Modifying
    @Query(value = "update posts set last_activity_at = created_at where last_activity_at is null",
            nativeQuery = true)
    int backfillActivityFromCreatedAt();
}
