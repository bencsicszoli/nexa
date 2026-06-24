package com.nexa.post;

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

import java.time.Instant;
import java.util.UUID;

/**
 * Egy szöveges bejegyzés. A #5 kártya csak szöveget kezel; a média (#6) a posztra
 * épülő külön táblában/oszlopban jön. A szerző a {@code users} táblára mutat.
 */
@Entity
@Table(name = "posts", indexes = {
        // A profil-időrend (szerző szerint, legfrissebb felül) gyakori lekérdezés.
        @Index(name = "idx_posts_author_created", columnList = "author_id, created_at")
})
public class Post {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** A bejegyzés szövege. Hosszú szöveg → {@code text} oszlop. */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Post() {
        // JPA
    }

    public Post(User author, String content) {
        this.author = author;
        this.content = content;
    }

    public UUID getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
