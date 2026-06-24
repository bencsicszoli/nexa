package com.nexa.post.dto;

import com.nexa.post.Post;
import com.nexa.user.User;

import java.time.Instant;

/**
 * A frontendnek visszaadott bejegyzés a szerző megjelenítéséhez szükséges
 * (denormalizált) szerzői adatokkal együtt.
 */
public record PostDto(
        String id,
        String authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        Instant createdAt) {

    public static PostDto from(Post post) {
        User author = post.getAuthor();
        return new PostDto(
                post.getId().toString(),
                author.getId().toString(),
                author.getDisplayName(),
                author.getAvatarUrl(),
                post.getContent(),
                post.getCreatedAt());
    }
}
