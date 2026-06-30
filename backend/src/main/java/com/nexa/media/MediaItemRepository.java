package com.nexa.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A médiatár-elemek tárháza. */
public interface MediaItemRepository extends JpaRepository<MediaItem, UUID> {

    /** Egy felhasználó médiatára időrendben, legfrissebb felül. */
    List<MediaItem> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    /** Egy elem a tulajdonos ellenőrzésével (idegen elem → üres, a létezést sem szivárogtatjuk). */
    Optional<MediaItem> findByIdAndOwnerId(UUID id, UUID ownerId);
}
