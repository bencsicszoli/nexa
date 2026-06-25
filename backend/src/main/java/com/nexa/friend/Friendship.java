package com.nexa.friend;

import com.nexa.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Kétirányú ismerősi kapcsolat egy kezdeményező és egy címzett között.
 * A pár között legfeljebb egy rekord létezik (irányonként egyedi); a fordított irányú
 * kérést a {@code FriendService} szándékosan megakadályozza. Az érzékeny kapcsolati adat
 * a PostgreSQL {@code friendships} táblába kerül (lásd CLAUDE.md adatbázis-elv).
 */
@Entity
@Table(name = "friendships",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_friendship_pair", columnNames = {"requester_id", "addressee_id"}),
        indexes = {
                @Index(name = "idx_friendship_addressee", columnList = "addressee_id, status"),
                @Index(name = "idx_friendship_requester", columnList = "requester_id, status")
        })
public class Friendship {

    @Id
    @GeneratedValue
    private UUID id;

    /** A kérést kezdeményező felhasználó. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** A kérés címzettje (ő fogadhatja el vagy utasíthatja el). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status = FriendshipStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Az elfogadás időpontja (PENDING állapotban null). */
    @Column(name = "responded_at")
    private Instant respondedAt;

    protected Friendship() {
        // JPA
    }

    public Friendship(User requester, User addressee) {
        this.requester = requester;
        this.addressee = addressee;
    }

    /** A kérés elfogadása — PENDING → ACCEPTED, az elfogadás idejének rögzítésével. */
    public void accept(Instant when) {
        this.status = FriendshipStatus.ACCEPTED;
        this.respondedAt = when;
    }

    public UUID getId() {
        return id;
    }

    public User getRequester() {
        return requester;
    }

    public User getAddressee() {
        return addressee;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }
}
