package com.nexa.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /**
     * Egy bejegyzés összes hozzászólása és válasza, létrehozási sorrendben. A szolgáltatás
     * ebből építi fel a fát (szülő → válaszok). Tipikus poszt-kommentszámnál ez elegendő;
     * lapozás később, ha a mérés indokolja.
     */
    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    long countByPostId(UUID postId);

    /** Poszt törlésénél: előbb a comment_media sorokat kell eltávolítani (FK-sorrend). */
    @Modifying
    @Query(value = "DELETE FROM comment_media WHERE comment_id IN (SELECT id FROM comments WHERE post_id = :postId)", nativeQuery = true)
    void deleteCommentMediaByPostId(@Param("postId") UUID postId);

    /** Poszt törlésénél: az összes hozzászólás törlése egy utasításban (a self-ref parent_id belső). */
    @Modifying
    @Query(value = "DELETE FROM comments WHERE post_id = :postId", nativeQuery = true)
    void deleteAllByPostId(@Param("postId") UUID postId);
}
