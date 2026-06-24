package com.nexa.profile.dto;

import jakarta.validation.constraints.NotBlank;

/** Avatar feltöltési link kérése; a {@code contentType} a kép MIME-típusa. */
public record AvatarUploadRequest(@NotBlank String contentType) {
}
