package com.nexa.post;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Egy bejegyzéshez csatolt média metaadata (URL + típus + méret). A bájtok az
 * objektumtárolóban élnek (presigned feltöltés); a DB csak a hivatkozást tárolja.
 * A {@code posts}-on belül rendezett {@code @ElementCollection}-ként szerepel
 * (lásd {@link Post}), így a {@code post_media} táblába kerül.
 */
@Embeddable
public class PostMedia {

    @Column(name = "url", nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private MediaType type;

    /** A fájl mérete bájtban (a megjelenítéshez/diagnosztikához; a DoD kéri). */
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    protected PostMedia() {
        // JPA
    }

    public PostMedia(String url, MediaType type, long sizeBytes) {
        this.url = url;
        this.type = type;
        this.sizeBytes = sizeBytes;
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
}
