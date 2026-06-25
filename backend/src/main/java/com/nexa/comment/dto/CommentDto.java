package com.nexa.comment.dto;

import com.nexa.comment.Comment;

import java.time.Instant;
import java.util.List;

/**
 * Egy hozzászólás vagy válasz a frontendnek, a beágyazott válaszaival együtt (fa). A {@code parentId}
 * null a közvetlen hozzászólásnál; az {@code editedAt} nem null, ha a kommentet szerkesztették
 * (a UI „szerkesztve" jelzéshez). A {@code replies} a közvetlen válaszok, létrehozási sorrendben.
 */
public record CommentDto(
        String id,
        String postId,
        String parentId,
        String authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        Instant createdAt,
        Instant editedAt,
        List<CommentDto> replies) {

    public static CommentDto of(Comment c, List<CommentDto> replies) {
        return new CommentDto(
                c.getId().toString(),
                c.getPost().getId().toString(),
                c.getParent() == null ? null : c.getParent().getId().toString(),
                c.getAuthor().getId().toString(),
                c.getAuthor().getDisplayName(),
                c.getAuthor().getAvatarUrl(),
                c.getContent(),
                c.getCreatedAt(),
                c.getEditedAt(),
                replies);
    }
}
