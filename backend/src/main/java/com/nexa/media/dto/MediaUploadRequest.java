package com.nexa.media.dto;

import jakarta.validation.constraints.NotBlank;

/** Médiatár-feltöltési link kérése; a {@code contentType} a fájl MIME-típusa. */
public record MediaUploadRequest(@NotBlank String contentType) {
}
