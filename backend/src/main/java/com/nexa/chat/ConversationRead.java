package com.nexa.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Egy felhasználó olvasottság-jelzője egy szálban (#12): meddig olvasta el az üzeneteket.
 * Az olvasatlan-számláló = a {@code lastReadAt} utáni, nem a felhasználótól származó üzenetek
 * száma. Mindkét szál-fajtánál (DIRECT és GROUP) egységesen ezt használjuk, így a csoport
 * olvasottsága is megoldott a {@link ConversationParticipant}-tól függetlenül.
 *
 * <p>Szándékosan nyers id-mezőket tárol (nem {@code @ManyToOne}), mert ez csak egy gyorsan
 * upsertelt jelző — entitás-betöltésre nincs szükség.
 */
@Entity
@Table(
        name = "conversation_reads",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
public class ConversationRead {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt;

    protected ConversationRead() {
        // JPA
    }

    public ConversationRead(UUID conversationId, UUID userId, Instant lastReadAt) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.lastReadAt = lastReadAt;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(Instant lastReadAt) {
        this.lastReadAt = lastReadAt;
    }
}
