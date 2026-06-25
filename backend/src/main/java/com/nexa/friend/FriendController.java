package com.nexa.friend;

import com.nexa.friend.dto.FriendDto;
import com.nexa.friend.dto.FriendRequestsDto;
import com.nexa.friend.dto.SendFriendRequestRequest;
import com.nexa.friend.dto.UserSummaryDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Ismerős-végpontok az {@code /api/friends} prefix alatt — mind hitelesítést igényel.
 * Kérés küldése / elfogadása / elutasítása-visszavonása, ismerőslista, függő kérések és
 * a kapcsolatállapottal feldúsított felhasználó-böngészés.
 */
@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    /** A bejelentkezett felhasználó elfogadott ismerősei. */
    @GetMapping
    public List<FriendDto> friends(@AuthenticationPrincipal UUID userId) {
        return friendService.listFriends(userId);
    }

    /** Függőben lévő kérések: beérkezett (elfogadható) és elküldött (visszavonható). */
    @GetMapping("/requests")
    public FriendRequestsDto requests(@AuthenticationPrincipal UUID userId) {
        return friendService.listRequests(userId);
    }

    /** Felhasználók böngészése a hívóhoz viszonyított kapcsolatállapottal (kérésküldéshez). */
    @GetMapping("/people")
    public List<UserSummaryDto> people(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(name = "query", required = false) String query) {
        return friendService.browsePeople(userId, query);
    }

    /** Ismerőskérés küldése egy másik felhasználónak. */
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendRequest(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SendFriendRequestRequest request) {
        friendService.sendRequest(userId, request.userId());
    }

    /** Egy beérkezett kérés elfogadása. */
    @PostMapping("/requests/{requestId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@AuthenticationPrincipal UUID userId, @PathVariable UUID requestId) {
        friendService.acceptRequest(userId, requestId);
    }

    /** Egy függő kérés elutasítása (címzettként) vagy visszavonása (kezdeményezőként). */
    @DeleteMapping("/requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRequest(@AuthenticationPrincipal UUID userId, @PathVariable UUID requestId) {
        friendService.removeRequest(userId, requestId);
    }

    /** Egy ismerős eltávolítása. */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(
            @AuthenticationPrincipal UUID userId,
            @PathVariable("userId") UUID otherUserId) {
        friendService.removeFriend(userId, otherUserId);
    }
}
