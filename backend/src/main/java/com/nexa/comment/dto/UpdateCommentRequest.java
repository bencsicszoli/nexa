package com.nexa.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Egy hozzászólás/válasz szövegének szerkesztése. */
public record UpdateCommentRequest(
        @NotBlank @Size(max = 2000) String content) {
}
