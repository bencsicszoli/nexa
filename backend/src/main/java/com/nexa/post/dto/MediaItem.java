package com.nexa.post.dto;

import com.nexa.post.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Egy előzőleg presigned URL-re feltöltött média hivatkozása létrehozáskor (bejegyzéshez vagy
 * hozzászóláshoz). A {@code key} a tárolóbeli kulcs, a {@code type}/{@code sizeBytes} a megjelenítéshez.
 */
public record MediaItem(
        @NotBlank String key,
        @NotNull MediaType type,
        @PositiveOrZero long sizeBytes) {
}
