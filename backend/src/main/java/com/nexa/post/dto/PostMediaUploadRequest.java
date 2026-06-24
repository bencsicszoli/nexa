package com.nexa.post.dto;

import jakarta.validation.constraints.NotBlank;

/** Poszt-média feltöltési link kérése; a {@code contentType} a fájl MIME-típusa. */
public record PostMediaUploadRequest(@NotBlank String contentType) {
}
