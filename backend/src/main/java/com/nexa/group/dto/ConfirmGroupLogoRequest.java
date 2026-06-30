package com.nexa.group.dto;

import jakarta.validation.constraints.NotBlank;

/** Meglévő csoport logójának megerősítésekor küldött adat; a {@code key} a feltöltött objektum tárolóbeli kulcsa. */
public record ConfirmGroupLogoRequest(@NotBlank String key) {
}
