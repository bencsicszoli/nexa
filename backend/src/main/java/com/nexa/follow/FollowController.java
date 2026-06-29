package com.nexa.follow;

import com.nexa.follow.dto.FollowUserDto;
import com.nexa.subscription.SubscriptionRequired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Követés-végpontok az {@code /api/follows} prefix alatt — mind hitelesítést igényel.
 * A követés egyirányú és idempotens (lásd {@link FollowService}); a {@code PUT}/{@code DELETE}
 * a követett felhasználó azonosítóján egy erőforrás (a követés) be-/kikapcsolása.
 */
@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    /** Akiket a bejelentkezett felhasználó követ. */
    @GetMapping("/following")
    public List<FollowUserDto> following(@AuthenticationPrincipal UUID userId) {
        return followService.listFollowing(userId);
    }

    /** Akik a bejelentkezett felhasználót követik. */
    @GetMapping("/followers")
    public List<FollowUserDto> followers(@AuthenticationPrincipal UUID userId) {
        return followService.listFollowers(userId);
    }

    /** Egy felhasználó követése (idempotens). */
    @PutMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SubscriptionRequired
    public void follow(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("userId") UUID followeeId) {
        followService.follow(userId, followeeId);
    }

    /** Egy felhasználó lekövetése (idempotens). */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("userId") UUID followeeId) {
        followService.unfollow(userId, followeeId);
    }
}
