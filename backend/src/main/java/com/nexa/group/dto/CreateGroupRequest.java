package com.nexa.group.dto;

import com.nexa.group.GroupVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Új csoport létrehozásakor küldött adat. A név kötelező; a leírás opcionális. A láthatóság
 * opcionális — ha nincs megadva, {@link GroupVisibility#PUBLIC} (bárki csatlakozhat). A
 * {@code logoKey} opcionális: egy korábban presigned URL-re feltöltött logó tárolóbeli kulcsa
 * (lásd {@code GroupService}); ha nincs, a csoport monogramos helyőrző logót kap a frontenden.
 */
public record CreateGroupRequest(
        @NotBlank @Size(min = 2, max = 80) String name,
        @Size(max = 500) String description,
        GroupVisibility visibility,
        String logoKey) {

    /** A láthatóság, alapértelmezetten PUBLIC, ha a kliens nem adta meg. */
    public GroupVisibility visibilityOrDefault() {
        return visibility == null ? GroupVisibility.PUBLIC : visibility;
    }
}
