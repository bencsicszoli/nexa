package com.nexa.post;

import com.nexa.post.dto.CreatePostRequest;
import com.nexa.post.dto.PostDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
 * A #5 kártya: létrehozás + saját profil időrendje. A hírfolyam (#10) külön végponton jön.
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
    public PostDto create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreatePostRequest request) {
        return postService.create(userId, request.content());
    }

    /** A bejelentkezett felhasználó saját bejegyzései. */
    @GetMapping("/me")
    public List<PostDto> myPosts(@AuthenticationPrincipal UUID userId) {
        return postService.listByAuthor(userId);
    }

    /** Egy adott felhasználó bejegyzései (pl. más profiljának megtekintéséhez). */
    @GetMapping("/user/{userId}")
    public List<PostDto> userPosts(@PathVariable UUID userId) {
        return postService.listByAuthor(userId);
    }
}
