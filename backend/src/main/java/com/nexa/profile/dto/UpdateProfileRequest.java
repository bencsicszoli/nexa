package com.nexa.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A profil szerkesztésekor küldött adat. A {@code bio} elhagyható. */
public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 50) String displayName,
        @Size(max = 280) String bio) {
}
