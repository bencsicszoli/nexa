package com.nexa.post.dto;

import jakarta.validation.Valid;
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
}
