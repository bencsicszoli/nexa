package com.nexa.auth;

import com.nexa.auth.dto.AuthResponse;
import com.nexa.auth.dto.Login2faRequest;
import com.nexa.auth.dto.LoginRequest;
import com.nexa.auth.dto.LoginResult;
import com.nexa.auth.dto.RefreshRequest;
import com.nexa.auth.dto.RegisterRequest;
import com.nexa.auth.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Hitelesítési végpontok az {@code /api/auth} prefix alatt.
 * A /register, /login, /refresh, /logout publikus; a /me védett (JWT kell hozzá).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Bejelentkezés. 2FA nélkül a token-párost adja vissza; ha a 2FA be van kapcsolva, helyette
     * {@code {twoFactorRequired:true, challengeToken}}-t — a kódot a {@code /login/2fa} váltja tokenre.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        if (result.twoFactorRequired()) {
            return ResponseEntity.ok(Map.of(
                    "twoFactorRequired", true,
                    "challengeToken", result.challengeToken()));
        }
        return ResponseEntity.ok(result.auth());
    }

    /** A kétlépcsős login második lépése: challenge token + 2FA kód → token-páros. */
    @PostMapping("/login/2fa")
    public AuthResponse loginWith2fa(@Valid @RequestBody Login2faRequest request) {
        return authService.loginWith2fa(request.challengeToken(), request.code());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) RefreshRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal UUID userId) {
        return authService.currentUser(userId);
    }
}
