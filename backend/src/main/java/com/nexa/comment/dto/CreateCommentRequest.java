package com.nexa.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Új hozzászólás vagy válasz. A {@code parentId} null/üres egy közvetlen hozzászólásnál; kitöltve
 * egy meglévő komment azonosítója, amelyre a válasz érkezik.
 */
public record CreateCommentRequest(
        @NotBlank @Size(max = 2000) String content,
        String parentId) {
}
