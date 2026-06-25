package com.nexa.group;

import com.nexa.user.User;
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
 * Egy csoport (#9): névvel, opcionális leírással és egy létrehozóval. A létrehozó
 * automatikusan admin (lásd {@link GroupMember}); a tagság a {@code group_members}
 * táblában él. A {@code groups} foglalt szó több SQL-dialektusban (H2/PostgreSQL is),
 * ezért a táblanevet backtick-kel idézzük — a Hibernate ezt a dialektus saját
 * idézőjelére fordítja.
 */
@Entity
@Table(name = "`groups`")
public class Group {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 80)
    private String name;

    /** Rövid leírás a csoportról (opcionális). */
    @Column(length = 500)
    private String description;

    /**
     * Láthatóság / csatlakozási mód. A {@code default 'PUBLIC'} azért kell, hogy a már
     * létező (e mező bevezetése előtti) csoportsorok is érvényes értéket kapjanak az
     * automatikus séma-frissítéskor.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'PUBLIC'")
    private GroupVisibility visibility = GroupVisibility.PUBLIC;

    /** A csoport létrehozója (egyben az első admin). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Group() {
        // JPA
    }

    public Group(String name, String description, GroupVisibility visibility, User createdBy) {
        this.name = name;
        this.description = description;
        this.visibility = visibility;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GroupVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(GroupVisibility visibility) {
        this.visibility = visibility;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
