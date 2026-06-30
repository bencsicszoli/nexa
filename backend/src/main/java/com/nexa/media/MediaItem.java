package com.nexa.media;

import com.nexa.post.MediaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A felhasználó személyes médiatárának egy eleme: a bejegyzésektől <b>független</b>
 * feltöltött kép/videó. A bájtok az objektumtárolóban élnek (presigned feltöltés, a
 * {@code library/} prefix alatt); a DB csak a publikus URL-t + típust + méretet tárolja.
 *
 * <p>Megosztáskor a kliens a fájlt újra feltölti egy poszt/komment saját másolataként
 * (lásd terv), így a médiatár-elem és a posztok életciklusa független — egy elem törlése
 * nem érinti a korábban megosztott posztokat, és fordítva.
 */
@Entity
@Table(name = "media_items", indexes = {
        // A médiatár-listázás tulajdonos szerint, legfrissebb felül megy.
        @Index(name = "idx_media_items_owner_created", columnList = "owner_id, created_at")
})
public class MediaItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "url", nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private MediaType type;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected MediaItem() {
        // JPA
    }

    public MediaItem(UUID ownerId, String url, MediaType type, long sizeBytes) {
        this.ownerId = ownerId;
        this.url = url;
        this.type = type;
        this.sizeBytes = sizeBytes;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getUrl() {
        return url;
    }

    public MediaType getType() {
        return type;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
