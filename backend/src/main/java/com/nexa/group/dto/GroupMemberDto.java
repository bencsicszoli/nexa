package com.nexa.group.dto;

import com.nexa.group.GroupMember;

import java.time.Instant;

/**
 * Egy csoporttag a tagok listájához: a felhasználó megjelenítési adatai + a szerepe és
 * a csatlakozás időpontja.
 */
public record GroupMemberDto(
        String id,
        String displayName,
        String avatarUrl,
        String role,
        Instant joinedAt) {

    public static GroupMemberDto of(GroupMember member) {
        return new GroupMemberDto(
                member.getUser().getId().toString(),
                member.getUser().getDisplayName(),
                member.getUser().getAvatarUrl(),
                member.getRole().name(),
                member.getJoinedAt());
    }
}
