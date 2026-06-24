package com.nexa.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Új szöveges bejegyzés létrehozásakor küldött adat. */
public record CreatePostRequest(
        @NotBlank @Size(max = 5000) String content) {
}
