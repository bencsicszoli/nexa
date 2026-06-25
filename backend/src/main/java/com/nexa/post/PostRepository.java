package com.nexa.post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    /**
     * Egy szerző profil-bejegyzései időrendben, legfrissebb felül (profil-időrend).
     * A csoportba írt posztok ({@code group} nem null) szándékosan kimaradnak — azok
     * a csoportoldalon jelennek meg, nem a szerző profilján.
     */
    List<Post> findByAuthorIdAndGroupIsNullOrderByCreatedAtDesc(UUID authorId);

    /** Egy csoport bejegyzései időrendben, legfrissebb felül (csoport-időrend, #9). */
    List<Post> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
