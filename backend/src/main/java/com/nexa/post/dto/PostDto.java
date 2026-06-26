package com.nexa.post.dto;

import com.nexa.post.MediaType;
import com.nexa.post.Post;
import com.nexa.user.User;

import java.time.Instant;
import java.util.List;

/**
 * A frontendnek visszaadott bejegyzés a szerző megjelenítéséhez szükséges
 * (denormalizált) szerzői adatokkal és a csatolt médiával együtt. A {@code group} csak akkor
 * van kitöltve, ha a bejegyzés egy csoporthoz tartozik (a hírfolyamban így jelezhető a
 * forráscsoport); profil-posztnál {@code null}.
 */
public record PostDto(
        String id,
        String authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        List<Media> media,
        Group group,
        Instant createdAt) {

    /** Egy csatolt média a megjelenítéshez (publikus URL + típus + méret). */
    public record Media(String url, MediaType type, long sizeBytes) {
    }

    /** A bejegyzés forráscsoportja a hírfolyam-jelöléshez (logó + név + link). */
    public record Group(String id, String name, String logoUrl) {
    }

    public static PostDto from(Post post) {
        User author = post.getAuthor();
        List<Media> media = post.getMedia().stream()
                .map(m -> new Media(m.getUrl(), m.getType(), m.getSizeBytes()))
                .toList();
        com.nexa.group.Group source = post.getGroup();
        Group group = source == null ? null
                : new Group(source.getId().toString(), source.getName(), source.getLogoUrl());
        return new PostDto(
                post.getId().toString(),
                author.getId().toString(),
                author.getDisplayName(),
                author.getAvatarUrl(),
                post.getContent(),
                media,
                group,
                post.getCreatedAt());
    }
}
