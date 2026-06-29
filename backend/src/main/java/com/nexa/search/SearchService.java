package com.nexa.search;

import com.nexa.group.GroupMember;
import com.nexa.group.GroupMemberRepository;
import com.nexa.group.GroupService;
import com.nexa.group.GroupVisibility;
import com.nexa.group.dto.GroupDto;
import com.nexa.post.PostRepository;
import com.nexa.post.dto.PostDto;
import com.nexa.search.dto.SearchResultsDto;
import com.nexa.search.dto.SearchUserDto;
import com.nexa.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Keresés (#16) a platformon: felhasználók, csoportok és bejegyzések egy keresőkifejezésre.
 * Mindhárom típusra kis/nagybetűtől független részsztring-illesztés (Postgres és a teszt-H2
 * között is hordozható; nagy adatnál {@code pg_trgm} GIN-indexszel gyorsítható). A találatokat
 * típusonként {@value #LIMIT} elemre korlátozzuk. A csoport-találatokat a {@link GroupService}
 * böngészéséből vesszük (a hívó tagsági szerepével), a bejegyzés-keresés pedig csak a hívónak
 * látható posztokat adja vissza (lásd {@link PostRepository#search}).
 */
@Service
public class SearchService {

    /** Találatok felső korlátja típusonként (egy keresőlapon). */
    private static final int LIMIT = 20;

    /** Üres tag-csoport halmaz helyett használt sentinel, hogy a JPQL {@code in} ne legyen üres. */
    private static final UUID NIL = new UUID(0L, 0L);

    private final UserRepository userRepository;
    private final GroupService groupService;
    private final GroupMemberRepository memberRepository;
    private final PostRepository postRepository;

    public SearchService(UserRepository userRepository, GroupService groupService,
                         GroupMemberRepository memberRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.groupService = groupService;
        this.memberRepository = memberRepository;
        this.postRepository = postRepository;
    }

    /**
     * Keresés mindhárom típusra. Üres/csak szóköz keresőkifejezésre üres találat (a frontend nem
     * is hív vele). A felhasználók közül a hívó saját magát kizárja.
     */
    @Transactional(readOnly = true)
    public SearchResultsDto search(UUID userId, String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return SearchResultsDto.empty();
        }
        PageRequest page = PageRequest.of(0, LIMIT);

        List<SearchUserDto> users = userRepository.search(q, userId, page).stream()
                .map(SearchUserDto::of)
                .toList();

        List<GroupDto> groups = groupService.browse(userId, q).stream()
                .limit(LIMIT)
                .toList();

        List<PostDto> posts = postRepository.search(q, myGroupIds(userId), GroupVisibility.PUBLIC, page).stream()
                .map(PostDto::from)
                .toList();

        return new SearchResultsDto(users, groups, posts);
    }

    /** A hívó tag-csoportjainak azonosítói; üres helyett a {@link #NIL} sentinel (lásd ott). */
    private List<UUID> myGroupIds(UUID userId) {
        List<UUID> ids = new ArrayList<>();
        for (GroupMember m : memberRepository.findByUserId(userId)) {
            ids.add(m.getGroup().getId());
        }
        if (ids.isEmpty()) {
            ids.add(NIL);
        }
        return ids;
    }
}
