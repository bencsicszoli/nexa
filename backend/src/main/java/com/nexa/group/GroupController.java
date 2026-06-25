package com.nexa.group;

import com.nexa.group.dto.CreateGroupRequest;
import com.nexa.group.dto.GroupDto;
import com.nexa.group.dto.GroupMemberDto;
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
}
