package com.nexa.group.dto;

import com.nexa.group.GroupJoinRequest;

import java.time.Instant;

/**
 * Egy függő csatlakozási kérelem az admin jóváhagyó nézetéhez: a kérelmező megjelenítési
 * adatai + a kérelem időpontja.
 */
public record GroupJoinRequestDto(
        String userId,
        String displayName,
        String avatarUrl,
        Instant requestedAt) {

    public static GroupJoinRequestDto of(GroupJoinRequest request) {
        return new GroupJoinRequestDto(
                request.getUser().getId().toString(),
                request.getUser().getDisplayName(),
                request.getUser().getAvatarUrl(),
                request.getCreatedAt());
    }
}
