package com.nexa.post.dto;

import com.nexa.post.MediaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Új bejegyzés létrehozásakor küldött adat. A szöveg és a média is opcionális, de a
 * kettő közül legalább az egyiket meg kell adni (ezt a {@code PostService} ellenőrzi).
 * A média elemek az előzőleg presigned URL-re feltöltött objektumok kulcsai.
 */
public record CreatePostRequest(
        @Size(max = 5000) String content,
        @Size(max = 10) @Valid List<MediaItem> media) {

    /** Egy feltöltött média hivatkozása a létrehozásnál. */
    public record MediaItem(
            @NotBlank String key,
            @NotNull MediaType type,
            @PositiveOrZero long sizeBytes) {
    }
}
