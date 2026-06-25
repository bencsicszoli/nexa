package com.nexa.comment.dto;

import com.nexa.post.dto.MediaItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Új hozzászólás vagy válasz. A szöveg és a média is opcionális, de legalább az egyiket meg kell
 * adni (ezt a {@code CommentService} ellenőrzi). A {@code parentId} null/üres egy közvetlen
 * hozzászólásnál; kitöltve egy meglévő komment azonosítója, amelyre a válasz érkezik.
 */
public record CreateCommentRequest(
        @Size(max = 2000) String content,
        @Size(max = 10) @Valid List<MediaItem> media,
        String parentId) {
}
