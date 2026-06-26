package com.nexa.user;

import com.nexa.user.dto.PublicUserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Felhasználói profilok az {@code /api/users} prefix alatt — hitelesítést igényel.
 * A nyilvános profilt a hívóhoz viszonyított kapcsolatállapottal együtt adja vissza; a
 * felhasználó bejegyzései a meglévő {@code GET /api/posts/user/{id}} végponton érhetők el.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Egy felhasználó nyilvános profilja a hívó szemszögéből. */
    @GetMapping("/{id}")
    public PublicUserDto getUser(@AuthenticationPrincipal UUID viewerId, @PathVariable UUID id) {
        return userService.getPublicProfile(viewerId, id);
    }
}
