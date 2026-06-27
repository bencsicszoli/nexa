package com.nexa.chat;

import com.nexa.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Egy résztvevő egy {@link ConversationType#DIRECT} szálban (#12). A DIRECT szálnak pontosan
 * két ilyen sora van — ezekből oldjuk fel a „másik felet" és a fan-out címzettjeit.
 *
 * <p>A {@link ConversationType#GROUP} szálaknak <b>nincs</b> ilyen soruk: ott a résztvevők a
 * csoport aktuális tagjai ({@code group_members}), így a tagság változása automatikusan
 * követi a csevegés résztvevőit. Az olvasottságot mindkét szál-fajtánál a
 * {@link ConversationRead} tartja számon.
 */
@Entity
@Table(
        name = "conversation_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
public class ConversationParticipant {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ConversationParticipant() {
        // JPA
    }

    public ConversationParticipant(Conversation conversation, User user) {
        this.conversation = conversation;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public User getUser() {
        return user;
    }
}
