package com.nexa.comment;

import com.nexa.comment.dto.CommentDto;
import com.nexa.comment.dto.CreateCommentRequest;
import com.nexa.comment.dto.UpdateCommentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Hozzászólás-végpontok (#9 kiegészítés) — mind hitelesítést igényel. A listázás/írás a
 * bejegyzéshez kötött ({@code /api/posts/{postId}/comments}); a szerkesztés/törlés a komment
 * azonosítóján ({@code /api/comments/{commentId}}).
 */
@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** Egy bejegyzés hozzászólás-fája (hozzászólások + beágyazott válaszok). */
    @GetMapping("/api/posts/{postId}/comments")
    public List<CommentDto> list(@PathVariable UUID postId) {
        return commentService.listForPost(postId);
    }

    /** Új hozzászólás vagy válasz egy bejegyzéshez. */
    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto create(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {
        return commentService.create(userId, postId, request.content(), request.parentId());
    }

    /** Egy saját hozzászólás/válasz szerkesztése. */
    @PatchMapping("/api/comments/{commentId}")
    public CommentDto update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.update(userId, commentId, request.content());
    }

    /** Egy hozzászólás/válasz törlése (szerző / posztoló / csoport-admin). */
    @DeleteMapping("/api/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID commentId) {
        commentService.delete(userId, commentId);
    }
}
