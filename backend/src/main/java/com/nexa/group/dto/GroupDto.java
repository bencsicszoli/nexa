package com.nexa.group.dto;

import com.nexa.group.Group;
import com.nexa.group.GroupRole;

import java.time.Instant;

/**
 * A frontendnek visszaadott csoport a böngészéshez és a csoportoldalhoz. A {@code role}
 * a hívó szerepe a csoportban — {@code null}, ha nem tagja (a UI ekkor „Csatlakozás"-t
 * ajánl); {@code "ADMIN"} a létrehozónak, {@code "MEMBER"} a többi tagnak.
 */
public record GroupDto(
        String id,
        String name,
        String description,
        long memberCount,
        String role,
        Instant createdAt) {

    public static GroupDto of(Group group, GroupRole role, long memberCount) {
        return new GroupDto(
                group.getId().toString(),
                group.getName(),
                group.getDescription(),
                memberCount,
                role == null ? null : role.name(),
                group.getCreatedAt());
    }
}
