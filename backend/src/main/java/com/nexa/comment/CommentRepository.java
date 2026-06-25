package com.nexa.comment;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
