package com.nexa.friend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    /**
     * A két felhasználó közötti kapcsolat iránytól függetlenül (ha létezik). Kérés
     * küldése előtt ezzel zárjuk ki a duplikált / fordított irányú kapcsolatot.
     */
    @Query("""
            select f from Friendship f
            where (f.requester.id = :a and f.addressee.id = :b)
               or (f.requester.id = :b and f.addressee.id = :a)
            """)
    Optional<Friendship> findBetween(@Param("a") UUID a, @Param("b") UUID b);

    /** Egy felhasználó összes kapcsolata (bármely irány, bármely állapot) — a relációk feltérképezéséhez. */
    @Query("""
            select f from Friendship f
            where f.requester.id = :userId or f.addressee.id = :userId
            """)
    List<Friendship> findAllForUser(@Param("userId") UUID userId);

    /** Egy felhasználó elfogadott ismerősi kapcsolatai, legutóbb elfogadott felül. */
    @Query("""
            select f from Friendship f
            where f.status = com.nexa.friend.FriendshipStatus.ACCEPTED
              and (f.requester.id = :userId or f.addressee.id = :userId)
            order by f.respondedAt desc
            """)
    List<Friendship> findAcceptedForUser(@Param("userId") UUID userId);

    /** A felhasználóhoz beérkezett, függőben lévő kérések (legfrissebb felül). */
    List<Friendship> findByAddresseeIdAndStatusOrderByCreatedAtDesc(UUID addresseeId, FriendshipStatus status);

    /** A felhasználó által elküldött, függőben lévő kérések (legfrissebb felül). */
    List<Friendship> findByRequesterIdAndStatusOrderByCreatedAtDesc(UUID requesterId, FriendshipStatus status);
}
