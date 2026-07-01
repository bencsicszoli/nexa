package com.nexa.profile.dto;

import jakarta.validation.constraints.NotBlank;

/** A feltöltött borítókép megerősítése a tárolótól kapott {@code key}-jel. */
public record ConfirmCoverRequest(@NotBlank String key) {
}
