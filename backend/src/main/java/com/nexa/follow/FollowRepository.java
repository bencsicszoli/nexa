package com.nexa.follow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    /** A konkrét (követő → követett) kapcsolat, ha létezik — követéskor/lekövetéskor. */
    Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    /** Akiket a felhasználó követ (legfrissebb követés felül). */
    @Query("""
            select f from Follow f
            where f.follower.id = :userId
            order by f.createdAt desc
            """)
    List<Follow> findFollowing(@Param("userId") UUID userId);

    /** Akik a felhasználót követik (legfrissebb követés felül). */
    @Query("""
            select f from Follow f
            where f.followee.id = :userId
            order by f.createdAt desc
            """)
    List<Follow> findFollowers(@Param("userId") UUID userId);

    /** Akiket a felhasználó követ — csak az id-k, a hírfolyam-aggregációhoz (#10). */
    @Query("select f.followee.id from Follow f where f.follower.id = :userId")
    List<UUID> findFolloweeIds(@Param("userId") UUID userId);
}
