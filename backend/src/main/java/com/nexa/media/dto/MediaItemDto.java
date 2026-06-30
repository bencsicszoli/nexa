package com.nexa.media.dto;

import com.nexa.media.MediaItem;
import com.nexa.post.MediaType;

import java.time.Instant;

/**
 * Egy médiatár-elem a frontendnek (publikus URL + típus + méret + létrehozás ideje).
 */
public record MediaItemDto(
        String id,
        String url,
        MediaType type,
        long sizeBytes,
        Instant createdAt) {

    public static MediaItemDto from(MediaItem item) {
        return new MediaItemDto(
                item.getId().toString(),
                item.getUrl(),
                item.getType(),
                item.getSizeBytes(),
                item.getCreatedAt());
    }
}
