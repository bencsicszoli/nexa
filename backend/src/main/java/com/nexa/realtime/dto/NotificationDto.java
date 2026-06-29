package com.nexa.realtime.dto;

import com.nexa.realtime.Notification;

import java.time.Instant;

/**
 * A klienshez tolt és az előzményben visszaadott értesítés (#11, #17). A {@code type} a
 * {@link com.nexa.realtime.NotificationType} neve (pl. {@code NEW_POST}, {@code FRIEND_REQUEST}).
 *
 * <p>A {@code post*}/{@code group*} mezők csak {@code NEW_POST}-nál vannak kitöltve (csoport-posztnál
 * a csoport is), a kapcsolati típusoknál {@code null}. A {@code read} mező a record végén
 * <b>additív</b> bővítés — nem töri a STOMP-deszerializációt a meglévő kliensen; az {@code id}
 * mostantól a perzisztált entitás azonosítója (kell az olvasott-jelöléshez).
 */
public record NotificationDto(
        String id,
        String type,
        String postId,
        String actorId,
        String actorName,
        String actorAvatarUrl,
        String groupId,
        String groupName,
        String groupLogoUrl,
        Instant createdAt,
        boolean read) {

    public static final String TYPE_NEW_POST = "NEW_POST";

    /** DTO egy perzisztált értesítésből (előzmény-lekérdezéshez és push-hoz egyaránt). */
    public static NotificationDto from(Notification n) {
        return new NotificationDto(
                n.getId().toString(),
                n.getType().name(),
                n.getPostId() == null ? null : n.getPostId().toString(),
                n.getActorId().toString(),
                n.getActorName(),
                n.getActorAvatarUrl(),
                n.getGroupId() == null ? null : n.getGroupId().toString(),
                n.getGroupName(),
                n.getGroupLogoUrl(),
                n.getCreatedAt(),
                n.isRead());
    }
}
