package com.nexa.user;

import com.nexa.common.ApiException;
import com.nexa.follow.FollowService;
import com.nexa.friend.FriendService;
import com.nexa.friend.FriendService.RelationshipView;
import com.nexa.user.dto.PublicUserDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Felhasználói profilok megtekintése más felhasználók számára. Összerakja a nyilvános
 * profiladatokat és a hívóhoz viszonyított kapcsolatállapotot (ismerős + követés), amiből a
 * frontend a megfelelő kapcsolat-műveletet ajánlja fel a profilon.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final FriendService friendService;
    private final FollowService followService;

    public UserService(UserRepository userRepository, FriendService friendService,
                       FollowService followService) {
        this.userRepository = userRepository;
        this.friendService = friendService;
        this.followService = followService;
    }

    /** Egy felhasználó nyilvános profilja a hívó szemszögéből (kapcsolatállapottal). */
    @Transactional(readOnly = true)
    public PublicUserDto getPublicProfile(UUID viewerId, UUID targetId) {
        User target = userRepository.findById(targetId).orElseThrow(ApiException::userNotFound);
        if (viewerId.equals(targetId)) {
            return PublicUserDto.of(target, true, "SELF", null, false);
        }
        RelationshipView rel = friendService.relationshipWith(viewerId, targetId);
        boolean following = followService.isFollowing(viewerId, targetId);
        return PublicUserDto.of(target, false, rel.status(), rel.requestId(), following);
    }
}
