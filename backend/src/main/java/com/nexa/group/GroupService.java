package com.nexa.group;

import com.nexa.common.ApiException;
import com.nexa.group.dto.CreateGroupRequest;
import com.nexa.group.dto.GroupDto;
import com.nexa.group.dto.GroupJoinRequestDto;
import com.nexa.group.dto.GroupMemberDto;
import com.nexa.post.Post;
import com.nexa.post.PostRepository;
import com.nexa.post.PostService;
import com.nexa.post.dto.CreatePostRequest;
import com.nexa.post.dto.PostDto;
import com.nexa.storage.DeferredStorageDeleter;
import com.nexa.storage.PresignedUpload;
import com.nexa.storage.StorageService;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Csoportok üzleti logikája (#9 + kiegészítések): létrehozás (létrehozó=admin, publikus/privát),
 * böngészés, saját csoportok, csatlakozás (publikusnál azonnal, privátnál kérelem), kilépés,
 * tagok, csatlakozási kérelmek kezelése (jóváhagyás/elutasítás), admin-moderáció (tag kizárása,
 * bejegyzés törlése), valamint a csoport posztjainak listája/írása. A bejegyzés-létrehozást/-törlést
 * a {@link PostService}-re delegáljuk (közös média-/validációs logika); a jogosultság itt dől el.
 */
@Service
public class GroupService {

    /** A böngészés egy lapjának felső korlátja (a teljes kereső a #16). */
    private static final int BROWSE_LIMIT = 50;

    /** A csoport-logók logikai mappája a tárolóban. */
    private static final String LOGO_PREFIX = "group-logos";
    private static final Set<String> ALLOWED_LOGO_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupJoinRequestRepository joinRequestRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostService postService;
    private final StorageService storageService;
    private final DeferredStorageDeleter storageDeleter;

    public GroupService(GroupRepository groupRepository, GroupMemberRepository memberRepository,
                        GroupJoinRequestRepository joinRequestRepository, UserRepository userRepository,
                        PostRepository postRepository, PostService postService,
                        StorageService storageService, DeferredStorageDeleter storageDeleter) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.postService = postService;
        this.storageService = storageService;
        this.storageDeleter = storageDeleter;
    }

    /** Aláírt logó-feltöltési cél a csoport-létrehozó űrlaphoz; csak képtípust enged. */
    public PresignedUpload createLogoUpload(String contentType) {
        String normalized = contentType == null ? "" : contentType.trim().toLowerCase();
        if (!ALLOWED_LOGO_TYPES.contains(normalized)) {
            throw ApiException.unsupportedImageType();
        }
        return storageService.createUpload(LOGO_PREFIX, normalized);
    }

    /** Aláírt logó-feltöltési cél egy meglévő csoport logójának frissítéséhez; csak admin. */
    public PresignedUpload createLogoUploadForGroup(UUID adminId, UUID groupId, String contentType) {
        requireAdmin(adminId, groupId);
        return createLogoUpload(contentType);
    }

    /** Meglévő csoport logójának frissítése feltöltött kulccsal; csak admin. */
    @Transactional
    public GroupDto updateLogo(UUID adminId, UUID groupId, String logoKey) {
        requireAdmin(adminId, groupId);
        Group group = groupRepository.findById(groupId).orElseThrow(ApiException::groupNotFound);
        String oldKey = storageService.keyFromPublicUrl(group.getLogoUrl());
        group.setLogoUrl(resolveLogoUrl(logoKey));
        storageDeleter.deleteAfterCommit(List.of(oldKey == null ? "" : oldKey));
        return toDto(group, adminId);
    }

    /** Meglévő csoport logójának eltávolítása; csak admin. */
    @Transactional
    public GroupDto removeLogo(UUID adminId, UUID groupId) {
        requireAdmin(adminId, groupId);
        Group group = groupRepository.findById(groupId).orElseThrow(ApiException::groupNotFound);
        String oldKey = storageService.keyFromPublicUrl(group.getLogoUrl());
        group.setLogoUrl(null);
        storageDeleter.deleteAfterCommit(List.of(oldKey == null ? "" : oldKey));
        return toDto(group, adminId);
    }

    /** Csoport létrehozása; a létrehozó azonnal admin tag lesz. Opcionális feltöltött logóval. */
    @Transactional
    public GroupDto create(UUID userId, CreateGroupRequest request) {
        User creator = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
        Group group = new Group(
                request.name().trim(), trimToNull(request.description()),
                request.visibilityOrDefault(), creator);
        group.setLogoUrl(resolveLogoUrl(request.logoKey()));
        groupRepository.save(group);
        memberRepository.save(new GroupMember(group, creator, GroupRole.ADMIN));
        return GroupDto.of(group, GroupRole.ADMIN, 1, false, 0);
    }

    /**
     * A feltöltött logó kulcsát publikus URL-lé oldja fel; csak a logó-mappába
     * ({@value #LOGO_PREFIX}/) mutató kulcsot fogad el. Üres/hiányzó kulcsnál {@code null}
     * (a csoport monogramos helyőrzőt kap).
     */
    private String resolveLogoUrl(String logoKey) {
        if (logoKey == null || logoKey.isBlank()) {
            return null;
        }
        if (!logoKey.startsWith(LOGO_PREFIX + "/")) {
            throw ApiException.invalidUpload();
        }
        return storageService.publicUrl(logoKey);
    }

    /**
     * Csoportok böngészése a hívó tagsági szerepével / kérelem-állapotával együtt. Üres szűrőre
     * az első {@value #BROWSE_LIMIT} csoport, név szerint.
     */
    @Transactional(readOnly = true)
    public List<GroupDto> browse(UUID userId, String query) {
        String q = query == null ? "" : query.trim();
        List<Group> groups = groupRepository.search(q, PageRequest.of(0, BROWSE_LIMIT));
        if (groups.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = groups.stream().map(Group::getId).toList();

        Map<UUID, GroupRole> myRoles = myRoles(userId);
        Set<UUID> myRequests = myRequestedGroupIds(userId);
        Map<UUID, Long> counts = memberCounts(ids);
        Map<UUID, Long> pending = pendingCounts(ids);

        return groups.stream()
                .map(g -> GroupDto.of(g, myRoles.get(g.getId()),
                        counts.getOrDefault(g.getId(), 0L),
                        myRequests.contains(g.getId()),
                        pending.getOrDefault(g.getId(), 0L)))
                .toList();
    }

    /** A bejelentkezett felhasználó csoportjai (legutóbb csatlakozott felül). */
    @Transactional(readOnly = true)
    public List<GroupDto> listMine(UUID userId) {
        List<GroupMember> memberships = memberRepository.findByUserIdOrderByJoinedAtDesc(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = memberships.stream().map(m -> m.getGroup().getId()).toList();
        Map<UUID, Long> counts = memberCounts(ids);
        Map<UUID, Long> pending = pendingCounts(ids);
        return memberships.stream()
                .map(m -> GroupDto.of(m.getGroup(), m.getRole(),
                        counts.getOrDefault(m.getGroup().getId(), 0L),
                        false,
                        pending.getOrDefault(m.getGroup().getId(), 0L)))
                .toList();
    }

    /** Egy csoport részletei a hívó szerepével / kérelem-állapotával. */
    @Transactional(readOnly = true)
    public GroupDto getGroup(UUID userId, UUID groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(ApiException::groupNotFound);
        return toDto(group, userId);
    }

    /** Egy csoport tagjai (adminok elöl). */
    @Transactional(readOnly = true)
    public List<GroupMemberDto> listMembers(UUID groupId) {
        requireGroup(groupId);
        return memberRepository.findByGroupIdOrderByRoleAscJoinedAtAsc(groupId).stream()
                .map(GroupMemberDto::of)
                .toList();
    }

    /**
     * Csatlakozás egy csoporthoz. Publikusnál azonnal taggá válik; privátnál csatlakozási
     * kérelem jön létre (az admin hagyja jóvá). Mindkettő idempotens.
     */
    @Transactional
    public GroupDto join(UUID userId, UUID groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(ApiException::groupNotFound);
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            User user = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
            if (group.getVisibility() == GroupVisibility.PUBLIC) {
                memberRepository.save(new GroupMember(group, user, GroupRole.MEMBER));
            } else if (!joinRequestRepository.existsByGroupIdAndUserId(groupId, userId)) {
                joinRequestRepository.save(new GroupJoinRequest(group, user));
            }
        }
        return toDto(group, userId);
    }

    /**
     * Kilépés egy csoportból. Idempotens, ha a hívó nem tag. Az utolsó admin nem léphet ki,
     * amíg más tagok vannak. (Egy függő csatlakozási kérelmet is visszavon, ha van.)
     */
    @Transactional
    public GroupDto leave(UUID userId, UUID groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(ApiException::groupNotFound);
        memberRepository.findByGroupIdAndUserId(groupId, userId).ifPresent(membership -> {
            if (membership.getRole() == GroupRole.ADMIN
                    && memberRepository.countByGroupIdAndRole(groupId, GroupRole.ADMIN) == 1
                    && memberRepository.countByGroupId(groupId) > 1) {
                throw ApiException.lastAdminCannotLeave();
            }
            memberRepository.delete(membership);
        });
        // Függő kérelem visszavonása (privát csoport, ha még nem tag).
        joinRequestRepository.findByGroupIdAndUserId(groupId, userId)
                .ifPresent(joinRequestRepository::delete);
        return toDto(group, userId);
    }

    /** Egy privát csoport függő csatlakozási kérelmei — csak az admin láthatja. */
    @Transactional(readOnly = true)
    public List<GroupJoinRequestDto> listJoinRequests(UUID adminId, UUID groupId) {
        requireAdmin(adminId, groupId);
        return joinRequestRepository.findByGroupIdOrderByCreatedAtAsc(groupId).stream()
                .map(GroupJoinRequestDto::of)
                .toList();
    }

    /** Egy csatlakozási kérelem jóváhagyása — a kérelmező taggá válik. Csak admin. */
    @Transactional
    public void approveJoinRequest(UUID adminId, UUID groupId, UUID requesterId) {
        requireAdmin(adminId, groupId);
        GroupJoinRequest request = joinRequestRepository.findByGroupIdAndUserId(groupId, requesterId)
                .orElseThrow(ApiException::joinRequestNotFound);
        if (!memberRepository.existsByGroupIdAndUserId(groupId, requesterId)) {
            memberRepository.save(new GroupMember(request.getGroup(), request.getUser(), GroupRole.MEMBER));
        }
        joinRequestRepository.delete(request);
    }

    /** Egy csatlakozási kérelem elutasítása. Csak admin. */
    @Transactional
    public void rejectJoinRequest(UUID adminId, UUID groupId, UUID requesterId) {
        requireAdmin(adminId, groupId);
        GroupJoinRequest request = joinRequestRepository.findByGroupIdAndUserId(groupId, requesterId)
                .orElseThrow(ApiException::joinRequestNotFound);
        joinRequestRepository.delete(request);
    }

    /** Egy tag kizárása — csak admin; admin tagot és saját magát nem lehet, a posztok maradnak. */
    @Transactional
    public void kickMember(UUID adminId, UUID groupId, UUID targetId) {
        requireAdmin(adminId, groupId);
        if (adminId.equals(targetId)) {
            throw ApiException.cannotKickSelf();
        }
        GroupMember target = memberRepository.findByGroupIdAndUserId(groupId, targetId)
                .orElseThrow(ApiException::targetNotGroupMember);
        if (target.getRole() == GroupRole.ADMIN) {
            throw ApiException.cannotKickAdmin();
        }
        memberRepository.delete(target);
    }

    /** Egy csoport bejegyzései időrendben (a csoport létezését ellenőrizzük). */
    @Transactional(readOnly = true)
    public List<PostDto> listPosts(UUID groupId) {
        requireGroup(groupId);
        return postService.listByGroup(groupId);
    }

    /** Bejegyzés írása a csoportba — csak tag teheti. */
    @Transactional
    public PostDto createPost(UUID userId, UUID groupId, CreatePostRequest request) {
        Group group = groupRepository.findById(groupId).orElseThrow(ApiException::groupNotFound);
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw ApiException.notGroupMember();
        }
        return postService.create(userId, request, group);
    }

    /** Egy csoport-bejegyzés törlése — a szerző vagy a csoport admin teheti (moderáció). */
    @Transactional
    public void deletePost(UUID userId, UUID groupId, UUID postId) {
        requireGroup(groupId);
        Post post = postRepository.findById(postId).orElseThrow(ApiException::postNotFound);
        if (post.getGroup() == null || !post.getGroup().getId().equals(groupId)) {
            throw ApiException.postNotFound();
        }
        boolean isAuthor = post.getAuthor().getId().equals(userId);
        if (!isAuthor && !isAdmin(userId, groupId)) {
            throw ApiException.notGroupAdmin();
        }
        postService.deleteAuthorized(post);
    }

    // --- segédek ---

    private GroupDto toDto(Group group, UUID userId) {
        GroupRole role = memberRepository.findByGroupIdAndUserId(group.getId(), userId)
                .map(GroupMember::getRole)
                .orElse(null);
        boolean requested = role == null
                && joinRequestRepository.existsByGroupIdAndUserId(group.getId(), userId);
        long pending = role == GroupRole.ADMIN
                ? joinRequestRepository.countByGroupId(group.getId()) : 0;
        return GroupDto.of(group, role, memberRepository.countByGroupId(group.getId()),
                requested, pending);
    }

    private Map<UUID, GroupRole> myRoles(UUID userId) {
        Map<UUID, GroupRole> roles = new HashMap<>();
        for (GroupMember m : memberRepository.findByUserId(userId)) {
            roles.put(m.getGroup().getId(), m.getRole());
        }
        return roles;
    }

    private Set<UUID> myRequestedGroupIds(UUID userId) {
        Set<UUID> ids = new HashSet<>();
        for (GroupJoinRequest r : joinRequestRepository.findByUserId(userId)) {
            ids.add(r.getGroup().getId());
        }
        return ids;
    }

    private Map<UUID, Long> memberCounts(List<UUID> groupIds) {
        return toCountMap(memberRepository.countByGroupIds(groupIds));
    }

    private Map<UUID, Long> pendingCounts(List<UUID> groupIds) {
        return toCountMap(joinRequestRepository.countByGroupIds(groupIds));
    }

    private static Map<UUID, Long> toCountMap(List<Object[]> rows) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    private boolean isAdmin(UUID userId, UUID groupId) {
        return memberRepository.findByGroupIdAndUserId(groupId, userId)
                .map(m -> m.getRole() == GroupRole.ADMIN)
                .orElse(false);
    }

    private void requireAdmin(UUID userId, UUID groupId) {
        requireGroup(groupId);
        if (!isAdmin(userId, groupId)) {
            throw ApiException.notGroupAdmin();
        }
    }

    private void requireGroup(UUID groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw ApiException.groupNotFound();
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
