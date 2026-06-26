package com.nexa.group.dto;

import jakarta.validation.constraints.NotBlank;

/** Csoport-logó feltöltési link kérése; a {@code contentType} a kép MIME-típusa. */
public record GroupLogoUploadRequest(@NotBlank String contentType) {
}
