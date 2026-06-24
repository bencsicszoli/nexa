package com.nexa.profile.dto;

import jakarta.validation.constraints.NotBlank;

/** A feltöltött avatar megerősítése a tárolótól kapott {@code key}-jel. */
public record ConfirmAvatarRequest(@NotBlank String key) {
}
