package com.nexa.follow;

import com.nexa.common.ApiException;
import com.nexa.follow.dto.FollowUserDto;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Egyirányú követés üzleti logikája (#8): követés, lekövetés, és a követett/követő listák.
 * <p>
 * A követés <b>idempotens</b>: meglévő követés újraküldése és nem létező követés lekövetése
 * is no-op — a UI gomb-toggle, és így a (kétszer kattintás, versenyhelyzet) sosem hibázik.
 * Saját magát senki nem követheti.
 */
@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    /** Egy felhasználó követése (idempotens — ha már követi, nem történik semmi). */
    @Transactional
    public void follow(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw ApiException.cannotFollowSelf();
        }
        User followee = userRepository.findById(followeeId).orElseThrow(ApiException::userNotFound);
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            return;
        }
        User follower = userRepository.findById(followerId).orElseThrow(ApiException::userNotFound);
        followRepository.save(new Follow(follower, followee));
    }

    /** Egy felhasználó lekövetése (idempotens — ha nem követi, nem történik semmi). */
    @Transactional
    public void unfollow(UUID followerId, UUID followeeId) {
        followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .ifPresent(followRepository::delete);
    }

    /** Akiket a bejelentkezett felhasználó követ (legfrissebb felül). */
    @Transactional(readOnly = true)
    public List<FollowUserDto> listFollowing(UUID userId) {
        return followRepository.findFollowing(userId).stream()
                .map(f -> FollowUserDto.of(f.getFollowee(), f.getCreatedAt()))
                .toList();
    }

    /** Akik a bejelentkezett felhasználót követik (legfrissebb felül). */
    @Transactional(readOnly = true)
    public List<FollowUserDto> listFollowers(UUID userId) {
        return followRepository.findFollowers(userId).stream()
                .map(f -> FollowUserDto.of(f.getFollower(), f.getCreatedAt()))
                .toList();
    }
}
