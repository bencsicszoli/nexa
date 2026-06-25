package com.nexa.comment;

import com.nexa.post.Post;
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
 * Egy hozzászólás vagy válasz egy bejegyzéshez (#9 kiegészítés). A {@code parent} null esetén
 * a komment közvetlenül a bejegyzésre adott <b>hozzászólás</b>; ha kitöltött, akkor a szülő
 * kommentre/válaszra adott <b>válasz</b> (Facebook-szerű, tetszőleges mélységű fa). Minden komment
 * a {@code post}-hoz tartozik (a fát ez alapján kérdezzük le). A {@code editedAt} a szerkesztés
 * tényét jelzi a UI-nak (különben null).
 */
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_post_created", columnList = "post_id, created_at"),
        @Index(name = "idx_comments_parent", columnList = "parent_id")
})
public class Comment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** A szülő komment, ha ez egy válasz; null a közvetlen (top-level) hozzászólásoknál. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** A legutóbbi szerkesztés időpontja, vagy null, ha sosem szerkesztették. */
    @Column(name = "edited_at")
    private Instant editedAt;

    protected Comment() {
        // JPA
    }

    public Comment(Post post, User author, Comment parent, String content) {
        this.post = post;
        this.author = author;
        this.parent = parent;
        this.content = content;
    }

    public UUID getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public User getAuthor() {
        return author;
    }

    public Comment getParent() {
        return parent;
    }

    public String getContent() {
        return content;
    }

    public void edit(String content, Instant editedAt) {
        this.content = content;
        this.editedAt = editedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEditedAt() {
        return editedAt;
    }
}
