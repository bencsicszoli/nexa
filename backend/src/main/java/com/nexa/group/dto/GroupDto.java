package com.nexa.group.dto;

import com.nexa.group.Group;
import com.nexa.group.GroupRole;

import java.time.Instant;

/**
 * A frontendnek visszaadott csoport a böngészéshez és a csoportoldalhoz. A {@code role} a hívó
 * szerepe a csoportban — {@code null}, ha nem tagja; {@code "ADMIN"} a létrehozónak,
 * {@code "MEMBER"} a többi tagnak. A {@code requested} igaz, ha a hívónak függő csatlakozási
 * kérelme van (privát csoport). A {@code pendingCount} csak adminnak releváns: a függő kérelmek
 * száma (nem-adminnál / nem-tagnál 0).
 */
public record GroupDto(
        String id,
        String name,
        String description,
        String visibility,
        long memberCount,
        String role,
        boolean requested,
        long pendingCount,
        Instant createdAt) {

    public static GroupDto of(Group group, GroupRole role, long memberCount,
                              boolean requested, long pendingCount) {
        return new GroupDto(
                group.getId().toString(),
                group.getName(),
                group.getDescription(),
                group.getVisibility().name(),
                memberCount,
                role == null ? null : role.name(),
                requested,
                role == GroupRole.ADMIN ? pendingCount : 0,
                group.getCreatedAt());
    }
}
