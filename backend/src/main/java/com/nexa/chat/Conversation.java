package com.nexa.chat;

import com.nexa.group.Group;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Egy csevegés-szál (#12): vagy kétszemélyes ({@link ConversationType#DIRECT}), vagy egy
 * csoporthoz kötött ({@link ConversationType#GROUP}). A tényleges üzenetek a
 * {@link ChatMessage} táblában élnek; itt csak a szál metaadata van.
 *
 * <p>A {@code lastMessageAt} denormalizált mező a beszélgetéslista rendezéséhez (legutóbb
 * aktív felül) — minden új üzenetnél frissül, így a lista egy lekérdezésből rendezhető.
 */
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    /** Csak {@link ConversationType#GROUP} szálnál kitöltve; DIRECT-nél {@code null}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    /** A legutóbbi üzenet ideje (rendezéshez); üres szálnál a létrehozás ideje. */
    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    /** A legutóbbi üzenet rövid előnézete a listához (denormalizált, üres szálnál {@code null}). */
    @Column(name = "last_message_preview", length = 200)
    private String lastMessagePreview;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Conversation() {
        // JPA
    }

    private Conversation(ConversationType type, Group group) {
        this.type = type;
        this.group = group;
        this.lastMessageAt = this.createdAt;
    }

    public static Conversation direct() {
        return new Conversation(ConversationType.DIRECT, null);
    }

    public static Conversation forGroup(Group group) {
        return new Conversation(ConversationType.GROUP, group);
    }

    public UUID getId() {
        return id;
    }

    public ConversationType getType() {
        return type;
    }

    public Group getGroup() {
        return group;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    /** Új üzenetnél frissíti a rendezési időt és a lista-előnézetet (max. 200 karakter). */
    public void recordMessage(Instant when, String preview) {
        this.lastMessageAt = when;
        if (preview != null && preview.length() > 200) {
            preview = preview.substring(0, 200);
        }
        this.lastMessagePreview = preview;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
