package com.nexa.post;

import com.nexa.post.dto.CreatePostRequest;
import com.nexa.post.dto.PostDto;
import com.nexa.post.dto.PostMediaUploadRequest;
import com.nexa.post.dto.UpdatePostRequest;
import com.nexa.storage.PresignedUpload;
import com.nexa.subscription.SubscriptionRequired;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Bejegyzés-végpontok az {@code /api/posts} prefix alatt — mind hitelesítést igényel.
 * Létrehozás (szöveg és/vagy média) + saját profil időrendje; a média presigned URL-re
 * tölt fel. A hírfolyam (#10) külön végponton jön.
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SubscriptionRequired
    public PostDto create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreatePostRequest request) {
        return postService.create(userId, request);
    }

    /** Aláírt feltöltési cél egy poszthoz csatolandó kép/videó számára. */
    @PostMapping("/media/upload-url")
    @SubscriptionRequired
    public PresignedUpload mediaUploadUrl(@Valid @RequestBody PostMediaUploadRequest request) {
        return postService.createMediaUpload(request.contentType());
    }

    /** A bejelentkezett felhasználó saját bejegyzései (legutóbbi aktivitás felül — új komment is). */
    @GetMapping("/me")
    public List<PostDto> myPosts(@AuthenticationPrincipal UUID userId) {
        return postService.listOwnByActivity(userId);
    }

    /** Egy adott felhasználó bejegyzései (pl. más profiljának megtekintéséhez). */
    @GetMapping("/user/{userId}")
    public List<PostDto> userPosts(@PathVariable UUID userId) {
        return postService.listByAuthor(userId);
    }

    /** Egy saját bejegyzés szövegének szerkesztése (a média változatlan). */
    @PatchMapping("/{id}")
    @SubscriptionRequired
    public PostDto update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request) {
        return postService.update(userId, id, request.content());
    }

    /** Egy saját bejegyzés törlése. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SubscriptionRequired
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        postService.delete(userId, id);
    }
}
