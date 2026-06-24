package com.nexa.post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    /** Egy szerző bejegyzései időrendben, legfrissebb felül (profil-időrend). */
    List<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);
}
