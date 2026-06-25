package com.nexa.group;

import com.nexa.group.dto.CreateGroupRequest;
import com.nexa.group.dto.GroupDto;
import com.nexa.group.dto.GroupJoinRequestDto;
import com.nexa.group.dto.GroupMemberDto;
import com.nexa.post.dto.CreatePostRequest;
import com.nexa.post.dto.PostDto;
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
 * Csoport-végpontok az {@code /api/groups} prefix alatt — mind hitelesítést igényel.
 * Létrehozás (létrehozó=admin), böngészés/saját csoportok, részletek és tagok,
 * csatlakozás/kilépés, valamint a csoport posztjainak listája/írása.
 */
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /** Új csoport létrehozása. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDto create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(userId, request);
    }

    /** Csoportok böngészése a hívó tagsági szerepével (csatlakozáshoz). */
    @GetMapping
    public List<GroupDto> browse(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(name = "query", required = false) String query) {
        return groupService.browse(userId, query);
    }

    /** A bejelentkezett felhasználó csoportjai. */
    @GetMapping("/mine")
    public List<GroupDto> mine(@AuthenticationPrincipal UUID userId) {
        return groupService.listMine(userId);
    }

    /** Egy csoport részletei. */
    @GetMapping("/{groupId}")
    public GroupDto get(@AuthenticationPrincipal UUID userId, @PathVariable UUID groupId) {
        return groupService.getGroup(userId, groupId);
    }

    /** Egy csoport tagjai (adminok elöl). */
    @GetMapping("/{groupId}/members")
    public List<GroupMemberDto> members(@PathVariable UUID groupId) {
        return groupService.listMembers(groupId);
    }

    /** Csatlakozás egy csoporthoz (idempotens). */
    @PostMapping("/{groupId}/join")
    public GroupDto join(@AuthenticationPrincipal UUID userId, @PathVariable UUID groupId) {
        return groupService.join(userId, groupId);
    }

    /** Kilépés egy csoportból (idempotens; az utolsó admin nem léphet ki). */
    @PostMapping("/{groupId}/leave")
    public GroupDto leave(@AuthenticationPrincipal UUID userId, @PathVariable UUID groupId) {
        return groupService.leave(userId, groupId);
    }

    /** Egy privát csoport függő csatlakozási kérelmei (csak admin). */
    @GetMapping("/{groupId}/requests")
    public List<GroupJoinRequestDto> joinRequests(
            @AuthenticationPrincipal UUID userId, @PathVariable UUID groupId) {
        return groupService.listJoinRequests(userId, groupId);
    }

    /** Egy csatlakozási kérelem jóváhagyása (csak admin). */
    @PostMapping("/{groupId}/requests/{requesterId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveRequest(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID groupId,
            @PathVariable UUID requesterId) {
        groupService.approveJoinRequest(userId, groupId, requesterId);
    }

    /** Egy csatlakozási kérelem elutasítása (csak admin). */
    @DeleteMapping("/{groupId}/requests/{requesterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectRequest(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID groupId,
            @PathVariable UUID requesterId) {
        groupService.rejectJoinRequest(userId, groupId, requesterId);
    }

    /** Egy tag kizárása a csoportból (csak admin). */
    @DeleteMapping("/{groupId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void kickMember(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID groupId,
            @PathVariable UUID memberId) {
        groupService.kickMember(userId, groupId, memberId);
    }

    /** Egy csoport bejegyzései időrendben. */
    @GetMapping("/{groupId}/posts")
    public List<PostDto> posts(@PathVariable UUID groupId) {
        return groupService.listPosts(groupId);
    }

    /** Bejegyzés írása a csoportba (csak tag). */
    @PostMapping("/{groupId}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDto createPost(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID groupId,
            @Valid @RequestBody CreatePostRequest request) {
        return groupService.createPost(userId, groupId, request);
    }

    /** Egy csoport-bejegyzés törlése (a szerző vagy a csoport admin — moderáció). */
    @DeleteMapping("/{groupId}/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID groupId,
            @PathVariable UUID postId) {
        groupService.deletePost(userId, groupId, postId);
    }
}
