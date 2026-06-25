package com.nexa.comment;

import com.nexa.comment.dto.CommentDto;
import com.nexa.common.ApiException;
import com.nexa.group.GroupMemberRepository;
import com.nexa.group.GroupRole;
import com.nexa.post.Post;
import com.nexa.post.PostRepository;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hozzászólások üzleti logikája (#9 kiegészítés): listázás (fa: hozzászólás → válaszok),
 * létrehozás (csoport-poszt alá csak tag), szerkesztés (csak a szerző) és törlés
 * (a szerző, a bejegyzés szerzője, vagy csoportposztnál a csoport admin — moderáció). Törléskor
 * a komment teljes részfája (a válaszai) is törlődik.
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository memberRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository,
                          UserRepository userRepository, GroupMemberRepository memberRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
    }

    /** Egy bejegyzés hozzászólás-fája (hozzászólások időrendben, alattuk a válaszok). */
    @Transactional(readOnly = true)
    public List<CommentDto> listForPost(UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw ApiException.postNotFound();
        }
        List<Comment> all = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        Map<UUID, List<Comment>> childrenByParent = new HashMap<>();
        List<Comment> roots = new ArrayList<>();
        for (Comment c : all) {
            if (c.getParent() == null) {
                roots.add(c);
            } else {
                childrenByParent.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c);
            }
        }
        return roots.stream().map(r -> toDto(r, childrenByParent)).toList();
    }

    /** Új hozzászólás (parentId null) vagy válasz (parentId egy meglévő komment ugyanezen poszton). */
    @Transactional
    public CommentDto create(UUID userId, UUID postId, String content, String parentId) {
        Post post = postRepository.findById(postId).orElseThrow(ApiException::postNotFound);
        // Csoport-poszt alá csak a csoport tagja kommentelhet (a profil-poszt bárkinek nyitott).
        if (post.getGroup() != null
                && !memberRepository.existsByGroupIdAndUserId(post.getGroup().getId(), userId)) {
            throw ApiException.notGroupMember();
        }
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.emptyComment();
        }

        Comment parent = resolveParent(parentId, postId);
        User author = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
        Comment saved = commentRepository.save(new Comment(post, author, parent, trimmed));
        return CommentDto.of(saved, List.of());
    }

    /** Egy hozzászólás/válasz szerkesztése — csak a szerző teheti; beállítja a „szerkesztve" jelzést. */
    @Transactional
    public CommentDto update(UUID userId, UUID commentId, String content) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(ApiException::commentNotFound);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw ApiException.commentNotFound();
        }
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.emptyComment();
        }
        comment.edit(trimmed, Instant.now());
        return CommentDto.of(comment, List.of());
    }

    /**
     * Egy hozzászólás/válasz törlése a teljes részfájával együtt. Törölheti a szerző, a bejegyzés
     * szerzője, illetve csoportposztnál a csoport admin (moderáció).
     */
    @Transactional
    public void delete(UUID userId, UUID commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(ApiException::commentNotFound);
        if (!canDelete(userId, comment)) {
            throw ApiException.commentNotFound();
        }
        commentRepository.deleteAllById(descendantIds(comment));
    }

    // --- segédek ---

    private CommentDto toDto(Comment c, Map<UUID, List<Comment>> childrenByParent) {
        List<CommentDto> replies = childrenByParent.getOrDefault(c.getId(), List.of()).stream()
                .map(child -> toDto(child, childrenByParent))
                .toList();
        return CommentDto.of(c, replies);
    }

    private Comment resolveParent(String parentId, UUID postId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        Comment parent = commentRepository.findById(UUID.fromString(parentId))
                .orElseThrow(ApiException::commentNotFound);
        if (!parent.getPost().getId().equals(postId)) {
            throw ApiException.invalidCommentParent();
        }
        return parent;
    }

    private boolean canDelete(UUID userId, Comment comment) {
        // A komment szerzője mindig törölheti a sajátját.
        if (comment.getAuthor().getId().equals(userId)) {
            return true;
        }
        Post post = comment.getPost();
        // Csoport-poszt: a moderáció kizárólag a csoport adminé — a poszt szerzőjének NINCS
        // ehhez joga (a csoport „tere" az adminé). Profil-poszt: a bejegyzés szerzője moderál
        // a saját posztja alatt.
        if (post.getGroup() != null) {
            return isGroupAdmin(userId, post.getGroup().getId());
        }
        return post.getAuthor().getId().equals(userId);
    }

    private boolean isGroupAdmin(UUID userId, UUID groupId) {
        return memberRepository.findByGroupIdAndUserId(groupId, userId)
                .map(m -> m.getRole() == GroupRole.ADMIN)
                .orElse(false);
    }

    /** A komment azonosítója + minden (tranzitív) válaszának azonosítója — a részfa törléséhez. */
    private List<UUID> descendantIds(Comment root) {
        List<Comment> all = commentRepository.findByPostIdOrderByCreatedAtAsc(root.getPost().getId());
        Map<UUID, List<UUID>> childIds = new HashMap<>();
        for (Comment c : all) {
            if (c.getParent() != null) {
                childIds.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c.getId());
            }
        }
        List<UUID> result = new ArrayList<>();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(root.getId());
        while (!stack.isEmpty()) {
            UUID id = stack.pop();
            result.add(id);
            childIds.getOrDefault(id, List.of()).forEach(stack::push);
        }
        return result;
    }
}
