package com.nexa.profile.dto;

import jakarta.validation.constraints.NotBlank;

/** Borítókép feltöltési link kérése; a {@code contentType} a kép MIME-típusa. */
public record CoverUploadRequest(@NotBlank String contentType) {
}
