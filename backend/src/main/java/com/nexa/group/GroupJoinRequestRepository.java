package com.nexa.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, UUID> {

    Optional<GroupJoinRequest> findByGroupIdAndUserId(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    /** Egy csoport függő kérelmei, legrégebbi felül (az admin sorban dolgozhatja fel). */
    List<GroupJoinRequest> findByGroupIdOrderByCreatedAtAsc(UUID groupId);

    long countByGroupId(UUID groupId);

    /** A felhasználó összes függő kérelme (a böngészésnél a „kérelmezve" állapothoz). */
    List<GroupJoinRequest> findByUserId(UUID userId);

    /** A megadott csoportok függő kérelmeinek száma egy lekérdezésben. Sorok: {@code [groupId, count]}. */
    @Query("""
            select r.group.id, count(r.id)
            from GroupJoinRequest r
            where r.group.id in :groupIds
            group by r.group.id
            """)
    List<Object[]> countByGroupIds(@Param("groupIds") List<UUID> groupIds);
}
