package com.nexa.follow;

import com.nexa.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Egyirányú követési kapcsolat (#8): a {@code follower} követi a {@code followee}-t —
 * tartalomgyártók, közéleti szereplők követésére. Az ismerősséggel ({@link com.nexa.friend.Friendship})
 * ellentétben ez nem kölcsönös és nincs elfogadás: a követés azonnal érvénybe lép.
 * <p>
 * Egy (követő, követett) párra legfeljebb egy rekord él (egyedi); az érzékeny kapcsolati
 * adat a PostgreSQL {@code follows} táblába kerül (lásd CLAUDE.md adatbázis-elv).
 */
@Entity
@Table(name = "follows",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_follow_pair", columnNames = {"follower_id", "followee_id"}),
        indexes = {
                @Index(name = "idx_follow_follower", columnList = "follower_id"),
                @Index(name = "idx_follow_followee", columnList = "followee_id")
        })
public class Follow {

    @Id
    @GeneratedValue
    private UUID id;

    /** A követő felhasználó (ő kezdeményezi/szünteti meg a követést). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    /** A követett felhasználó. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Follow() {
        // JPA
    }

    public Follow(User follower, User followee) {
        this.follower = follower;
        this.followee = followee;
    }

    public UUID getId() {
        return id;
    }

    public User getFollower() {
        return follower;
    }

    public User getFollowee() {
        return followee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
