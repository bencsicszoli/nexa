package com.nexa.comment.dto;

import com.nexa.comment.Comment;
import com.nexa.post.MediaType;

import java.time.Instant;
import java.util.List;

/**
 * Egy hozzászólás vagy válasz a frontendnek, a beágyazott válaszaival és a csatolt médiával együtt
 * (fa). A {@code parentId} null a közvetlen hozzászólásnál; az {@code editedAt} nem null, ha a
 * kommentet szerkesztették (a UI „szerkesztve" jelzéshez). A {@code replies} a közvetlen válaszok,
 * létrehozási sorrendben.
 */
public record CommentDto(
        String id,
        String postId,
        String parentId,
        String authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        List<Media> media,
        Instant createdAt,
        Instant editedAt,
        List<CommentDto> replies) {

    /** Egy csatolt média a megjelenítéshez (publikus URL + típus + méret). */
    public record Media(String url, MediaType type, long sizeBytes) {
    }

    public static CommentDto of(Comment c, List<CommentDto> replies) {
        List<Media> media = c.getMedia().stream()
                .map(m -> new Media(m.getUrl(), m.getType(), m.getSizeBytes()))
                .toList();
        return new CommentDto(
                c.getId().toString(),
                c.getPost().getId().toString(),
                c.getParent() == null ? null : c.getParent().getId().toString(),
                c.getAuthor().getId().toString(),
                c.getAuthor().getDisplayName(),
                c.getAuthor().getAvatarUrl(),
                c.getContent(),
                media,
                c.getCreatedAt(),
                c.getEditedAt(),
                replies);
    }
}
