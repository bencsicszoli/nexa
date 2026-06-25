package com.nexa.group;

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
 * Egy függőben lévő csatlakozási kérelem egy <b>privát</b> csoporthoz (#9 kiegészítés).
 * Az admin jóváhagyásakor {@link GroupMember} jön létre és a kérelem törlődik; elutasításkor
 * vagy a kérelmező visszavonásakor csak törlődik. Egy (csoport, felhasználó) párra legfeljebb
 * egy kérelem él (egyedi).
 */
@Entity
@Table(name = "group_join_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_group_join_request", columnNames = {"group_id", "user_id"}),
        indexes = @Index(name = "idx_group_join_request_group", columnList = "group_id"))
public class GroupJoinRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected GroupJoinRequest() {
        // JPA
    }

    public GroupJoinRequest(Group group, User user) {
        this.group = group;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public Group getGroup() {
        return group;
    }

    public User getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
