package com.nexa.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Új csoport létrehozásakor küldött adat. A név kötelező; a leírás opcionális.
 */
public record CreateGroupRequest(
        @NotBlank @Size(min = 2, max = 80) String name,
        @Size(max = 500) String description) {
}
