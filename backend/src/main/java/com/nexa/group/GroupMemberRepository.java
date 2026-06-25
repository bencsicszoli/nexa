package com.nexa.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    /** Egy konkrét tagság (csoport + felhasználó), ha létezik — csatlakozáskor/kilépéskor. */
    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    long countByGroupId(UUID groupId);

    long countByGroupIdAndRole(UUID groupId, GroupRole role);

    /** Egy csoport tagjai — adminok elöl, azon belül a csatlakozás sorrendjében. */
    List<GroupMember> findByGroupIdOrderByRoleAscJoinedAtAsc(UUID groupId);

    /** A felhasználó tagságai, legutóbb csatlakozott felül (a „Csoportjaim" listához). */
    List<GroupMember> findByUserIdOrderByJoinedAtDesc(UUID userId);

    /** A felhasználó összes tagsága (a böngészésnél a szerep gyors kikereséséhez). */
    List<GroupMember> findByUserId(UUID userId);

    /**
     * A megadott csoportok taglétszáma egy lekérdezésben (N+1 nélkül, a böngészéshez).
     * Minden sor: {@code [groupId, count]}.
     */
    @Query("""
            select gm.group.id, count(gm.id)
            from GroupMember gm
            where gm.group.id in :groupIds
            group by gm.group.id
            """)
    List<Object[]> countByGroupIds(@Param("groupIds") List<UUID> groupIds);
}
