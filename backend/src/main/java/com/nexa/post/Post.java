package com.nexa.post;

import com.nexa.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Egy bejegyzés: szöveg és/vagy csatolt média (kép/videó, #6 kártya). A médiák a
 * {@code post_media} táblában, rendezett listaként élnek; a bájtok az objektumtárolóban
 * vannak, a DB csak a metaadatot tárolja. A szerző a {@code users} táblára mutat.
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

    /**
     * A bejegyzés szövege. Hosszú szöveg → {@code text} oszlop. Csak médiát tartalmazó
     * posztnál üres string (nem null) — így a #5-ben létrejött NOT NULL megmaradhat.
     */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** A csatolt média a feltöltés sorrendjében (kép/videó); üres lehet. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_media", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "position")
    private List<PostMedia> media = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Post() {
        // JPA
    }

    public Post(User author, String content, List<PostMedia> media) {
        this.author = author;
        this.content = content;
        if (media != null) {
            this.media = media;
        }
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

    /** A szöveg módosítása (szerkesztés, #6 kiegészítés). A média változatlan marad. */
    public void setContent(String content) {
        this.content = content;
    }

    public List<PostMedia> getMedia() {
        return media;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
