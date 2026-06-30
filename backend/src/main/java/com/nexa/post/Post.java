package com.nexa.post;

import com.nexa.group.Group;
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
        @Index(name = "idx_posts_author_created", columnList = "author_id, created_at"),
        // A csoport-időrend (csoport szerint, legfrissebb felül) a csoportoldalhoz (#9).
        @Index(name = "idx_posts_group_created", columnList = "group_id, created_at"),
        // A hírfolyam-rendezés az utolsó aktivitáson megy (létrehozás vagy új komment).
        @Index(name = "idx_posts_last_activity", columnList = "last_activity_at")
})
public class Post {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * A csoport, amelybe a bejegyzés kerül (#9). {@code null} a „közönséges" (profil)
     * posztoknál; a profil-időrend csak ez utóbbiakat mutatja, a csoportoldal pedig
     * kizárólag az adott csoport posztjait.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

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

    /**
     * A bejegyzés utolsó aktivitásának ideje: létrehozáskor a {@code createdAt}, majd minden
     * <b>új hozzászólás</b> előbbre tolja ({@link #touchActivity}). A hírfolyam (#10) ezen
     * rendez, hogy a friss választ kapó bejegyzés a folyam tetejére kerüljön — a sorrend így is
     * tisztán időrendi (utolsó aktivitás szerint), nincs benne rangsoroló/ajánló logika.
     * Régi (oszlop előtti) sorokon {@code null} lehet — a lekérdezés és a {@link #getLastActivityAt}
     * ilyenkor a {@code createdAt}-ra esik vissza.
     */
    @Column(name = "last_activity_at")
    private Instant lastActivityAt = Instant.now();

    protected Post() {
        // JPA
    }

    public Post(User author, String content, List<PostMedia> media) {
        this(author, content, media, null);
    }

    public Post(User author, String content, List<PostMedia> media, Group group) {
        this.author = author;
        this.content = content;
        if (media != null) {
            this.media = media;
        }
        this.group = group;
    }

    public UUID getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    /** A csoport, amelybe a poszt tartozik, vagy {@code null}, ha profil-poszt. */
    public Group getGroup() {
        return group;
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

    /**
     * A hírfolyam-rendezés kulcsa: az utolsó aktivitás ideje (létrehozás vagy a legfrissebb
     * hozzászólás). Régi, oszlop nélkül létrejött sornál {@code null} helyett a {@code createdAt}-ot
     * adja vissza, így a rendezés sosem kap null kulcsot.
     */
    public Instant getLastActivityAt() {
        return lastActivityAt != null ? lastActivityAt : createdAt;
    }

    /**
     * Előbbre tolja az utolsó aktivitást (új hozzászóláskor), de csak előre — régebbi időpont
     * nem rontja le a már beállított értéket. Így a bejegyzés a hírfolyam tetejére kerül.
     */
    public void touchActivity(Instant at) {
        if (at != null && (lastActivityAt == null || at.isAfter(lastActivityAt))) {
            this.lastActivityAt = at;
        }
    }
}
