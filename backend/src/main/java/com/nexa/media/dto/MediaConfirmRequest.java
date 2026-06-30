package com.nexa.media.dto;

import com.nexa.post.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Egy presigned URL-re feltöltött médiatár-elem megerősítése: a {@code key} a
 * tárolóbeli kulcs, a {@code type}/{@code sizeBytes} a megjelenítéshez.
 */
public record MediaConfirmRequest(
        @NotBlank String key,
        @NotNull MediaType type,
        @PositiveOrZero long sizeBytes) {
}
