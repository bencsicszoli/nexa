package com.nexa.post.dto;

import com.nexa.post.MediaType;
import com.nexa.post.Post;
import com.nexa.user.User;

import java.time.Instant;
import java.util.List;

/**
 * A frontendnek visszaadott bejegyzés a szerző megjelenítéséhez szükséges
 * (denormalizált) szerzői adatokkal és a csatolt médiával együtt.
 */
public record PostDto(
        String id,
        String authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        List<Media> media,
        Instant createdAt) {

    /** Egy csatolt média a megjelenítéshez (publikus URL + típus + méret). */
    public record Media(String url, MediaType type, long sizeBytes) {
    }

    public static PostDto from(Post post) {
        User author = post.getAuthor();
        List<Media> media = post.getMedia().stream()
                .map(m -> new Media(m.getUrl(), m.getType(), m.getSizeBytes()))
                .toList();
        return new PostDto(
                post.getId().toString(),
                author.getId().toString(),
                author.getDisplayName(),
                author.getAvatarUrl(),
                post.getContent(),
                media,
                post.getCreatedAt());
    }
}
