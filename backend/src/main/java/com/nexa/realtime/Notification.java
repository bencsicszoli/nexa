package com.nexa.realtime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Egy perzisztált értesítés (#17). A korábbi „live-only" push helyett minden értesítés
 * előbb ide kerül (a kiváltó tranzakcióban), így az előzmény újratöltés után is látszik,
 * és olvasottság-állapota lehet. Az aktor adatait (név, avatar) <b>denormalizáltan</b>
 * tároljuk, hogy a lista megjelenítéséhez ne kelljen felhasználó-lekérdezés.
 *
 * <p>A {@code post*}/{@code group*} mezők típusfüggően vannak kitöltve: {@code NEW_POST}-nál
 * a posztra (csoport-posztnál a csoportra is) mutatnak, a kapcsolati típusoknál {@code null}.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_recipient_created", columnList = "recipient_id, created_at")
})
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    /** A címzett felhasználó id-ja (az ő előzményében és olvasatlan-számában jelenik meg). */
    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    // --- Denormalizált aktor-adat ---
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "actor_name", nullable = false)
    private String actorName;

    @Column(name = "actor_avatar_url")
    private String actorAvatarUrl;

    // --- Opcionális tartalmi hivatkozások (csak NEW_POST-nál) ---
    @Column(name = "post_id")
    private UUID postId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "group_logo_url")
    private String groupLogoUrl;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {
        // JPA
    }

    private Notification(UUID recipientId, NotificationType type, UUID actorId, String actorName,
                         String actorAvatarUrl, UUID postId, UUID groupId, String groupName,
                         String groupLogoUrl) {
        this.recipientId = recipientId;
        this.type = type;
        this.actorId = actorId;
        this.actorName = actorName;
        this.actorAvatarUrl = actorAvatarUrl;
        this.postId = postId;
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupLogoUrl = groupLogoUrl;
    }

    /** Új-bejegyzés értesítés (profil- vagy csoport-poszt; a group* mezők csoport-posztnál vannak). */
    public static Notification newPost(UUID recipientId, UUID actorId, String actorName,
                                       String actorAvatarUrl, UUID postId, UUID groupId,
                                       String groupName, String groupLogoUrl) {
        return new Notification(recipientId, NotificationType.NEW_POST, actorId, actorName,
                actorAvatarUrl, postId, groupId, groupName, groupLogoUrl);
    }

    /** Kapcsolati értesítés (kérés/elfogadás/követés) — nincs poszt/csoport hivatkozás. */
    public static Notification relationship(UUID recipientId, NotificationType type, UUID actorId,
                                            String actorName, String actorAvatarUrl) {
        return new Notification(recipientId, type, actorId, actorName, actorAvatarUrl,
                null, null, null, null);
    }

    public UUID getId() {
        return id;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public NotificationType getType() {
        return type;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getActorAvatarUrl() {
        return actorAvatarUrl;
    }

    public UUID getPostId() {
        return postId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getGroupLogoUrl() {
        return groupLogoUrl;
    }

    public boolean isRead() {
        return read;
    }

    public void markRead() {
        this.read = true;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
