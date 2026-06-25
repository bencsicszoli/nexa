package com.nexa.group;

import com.nexa.common.ApiException;
import com.nexa.group.dto.CreateGroupRequest;
import com.nexa.group.dto.GroupDto;
import com.nexa.group.dto.GroupMemberDto;
import com.nexa.post.PostService;
import com.nexa.post.dto.CreatePostRequest;
import com.nexa.post.dto.PostDto;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Csoportok üzleti logikája (#9): létrehozás (a létrehozó automatikusan admin), böngészés,
 * a saját csoportok, csatlakozás/kilépés, a csoport tagjai és posztjai, valamint a csoportba
 * írás. A bejegyzés-létrehozást a {@link PostService}-re delegáljuk (közös média-/validációs
 * logika); a tagság-ellenőrzés itt történik.
 */
@Service
public class GroupService {

    /** A böngészés egy lapjának felső korlátja (a teljes kereső a #16). */
    private static final int BROWSE_LIMIT = 50;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PostService postService;

    public GroupService(GroupRepository groupRepository, GroupMemberRepository memberRepository,
                        UserRepository userRepository, PostService postService) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.postService = postService;
    }

    /** Csoport létrehozása; a létrehozó azonnal admin tag lesz. */
    @Transactional
    public GroupDto create(UUID userId, CreateGroupRequest request) {
        User creator = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
        Group group = groupRepository.save(
                new Group(request.name().trim(), trimToNull(request.description()), creator));
        memberRepository.save(new GroupMember(group, creator, GroupRole.ADMIN));
        return GroupDto.of(group, GroupRole.ADMIN, 1);
    }

    /**
     * Csoportok böngészése a hívó tagsági szerepével együtt (hogy a UI „Csatlakozás"-t vagy
     * „Belépve"-t mutathasson). Üres szűrőre az első {@value #BROWSE_LIMIT} csoport, név szerint.
     */
    @Transactional(readOnly = true)
    public List<GroupDto> browse(UUID userId, String query) {
        String q = query == null ? "" : query.trim();
        List<Group> groups = groupRepository.search(q, PageRequest.of(0, BROWSE_LIMIT));
        if (groups.isEmpty()) {
            return List.of();
        }

        // A hívó szerepe csoportonként + a taglétszámok — egy-egy lekérdezésből (N+1 nélkül).
        Map<UUID, GroupRole> myRoles = new HashMap<>();
        for (GroupMember m : memberRepository.findByUserId(userId)) {
            myRoles.put(m.getGroup().getId(), m.getRole());
        }
        Map<UUID, Long> counts = countsByGroup(groups.stream().map(Group::getId).toList());

        return groups.stream()
                .map(g -> GroupDto.of(g, myRoles.get(g.getId()), counts.getOrDefault(g.getId(), 0L)))
                .toList();
    }

    /** A bejelentkezett felhasználó csoportjai (legutóbb csatlakozott felül). */
    @Transactional(readOnly = true)
    public List<GroupDto> listMine(UUID userId) {
        List<GroupMember> memberships = memberRepository.findByUserIdOrderByJoinedAtDesc(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> counts = countsByGroup(
                memberships.stream().map(m -> m.getGroup().getId()).toList());
        return memberships.stream()
                .map(m -> GroupDto.of(m.getGroup(), m.getRole(),
                        counts.getOrDefault(m.getGroup().getId(), 0L)))
                .toList();
    }

    /** Egy csoport részletei a hívó szerepével. */
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

    /** Csatlakozás egy csoporthoz (idempotens — ha már tag, nem történik semmi). */
    @Transactional
    public GroupDto join(UUID userId, UUID groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(ApiException::groupNotFound);
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            User user = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
            memberRepository.save(new GroupMember(group, user, GroupRole.MEMBER));
        }
        return toDto(group, userId);
    }

    /**
     * Kilépés egy csoportból. Idempotens, ha a hívó nem tag. Az utolsó admin nem léphet ki,
     * amíg más tagok vannak — különben a csoport admin nélkül maradna.
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
        return toDto(group, userId);
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

    // --- segédek ---

    private GroupDto toDto(Group group, UUID userId) {
        GroupRole role = memberRepository.findByGroupIdAndUserId(group.getId(), userId)
                .map(GroupMember::getRole)
                .orElse(null);
        return GroupDto.of(group, role, memberRepository.countByGroupId(group.getId()));
    }

    private Map<UUID, Long> countsByGroup(List<UUID> groupIds) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : memberRepository.countByGroupIds(groupIds)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
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
