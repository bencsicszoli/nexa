package com.nexa.realtime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Egy felhasználó értesítés-előzménye, legfrissebb felül (lapozva). */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    /** Az olvasatlan értesítések száma a harang-jelvényhez. */
    long countByRecipientIdAndReadFalse(UUID recipientId);

    /** Egy értesítés a tulajdonosi ellenőrzéssel (idegen id → üres). */
    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    /** Minden olvasatlan értesítés olvasottra állítása egy lekérdezésben. */
    @Transactional
    @Modifying
    @Query("update Notification n set n.read = true where n.recipientId = :recipientId and n.read = false")
    int markAllRead(@Param("recipientId") UUID recipientId);
}
