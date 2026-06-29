package com.nexa.auth;

import com.nexa.auth.dto.AuthResponse;
import com.nexa.auth.dto.LoginRequest;
import com.nexa.auth.dto.LoginResult;
import com.nexa.auth.dto.RegisterRequest;
import com.nexa.auth.dto.UserDto;
import com.nexa.common.ApiException;
import com.nexa.security.JwtService;
import com.nexa.user.User;
import com.nexa.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * A hitelesítés üzleti logikája: regisztráció, bejelentkezés, token-frissítés
 * (rotációval) és kijelentkezés (a refresh token visszavonásával).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TwoFactorService twoFactorService;
    private final long refreshTokenTtlSeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TwoFactorService twoFactorService,
            @Value("${nexa.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.twoFactorService = twoFactorService;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.emailAlreadyExists();
        }
        User user = new User(email, req.displayName().trim(), passwordEncoder.encode(req.password()));
        userRepository.save(user);
        return issueTokens(user);
    }

    /**
     * Bejelentkezés. Ha a felhasználónál be van kapcsolva a 2FA, NEM ad tokent, hanem egy rövid
     * életű challenge tokent ({@link LoginResult#challenge}); a tokeneket a {@link #loginWith2fa}
     * állítja ki a 2FA kód megadása után. 2FA nélkül a sima token-páros megy (#17).
     */
    @Transactional
    public LoginResult login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(ApiException::invalidCredentials);
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.invalidCredentials();
        }
        if (user.isTotpEnabled()) {
            return LoginResult.challenge(jwtService.generate2faChallengeToken(user.getId()));
        }
        return LoginResult.authenticated(issueTokens(user));
    }

    /** A kétlépcsős login második lépése: challenge token + 2FA kód → token-páros (#17). */
    @Transactional
    public AuthResponse loginWith2fa(String challengeToken, String code) {
        UUID userId;
        try {
            userId = jwtService.extract2faChallengeUserId(challengeToken);
        } catch (Exception e) {
            throw ApiException.invalidChallengeToken();
        }
        User user = userRepository.findById(userId).orElseThrow(ApiException::invalidChallengeToken);
        if (!user.isTotpEnabled() || !twoFactorService.verify(user, code)) {
            throw ApiException.invalid2faCode();
        }
        return issueTokens(user);
    }

    /**
     * Jelszóváltás: a jelenlegi jelszó ellenőrzése → új hash → az ÖSSZES refresh token visszavonása
     * (a felhasználó minden munkamenete megszakad, a sajátját is beleértve) (#17).
     */
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(ApiException::userNotFound);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.wrongPassword();
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        refreshTokenRepository.deleteByUser(user);
    }

    /** Refresh token beváltása: ellenőrzés → régi visszavonása (rotáció) → új páros. */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(ApiException::invalidRefreshToken);
        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw ApiException.invalidRefreshToken();
        }
        User user = stored.getUser();
        refreshTokenRepository.delete(stored);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenRepository.deleteByTokenHash(sha256(rawRefreshToken));
        }
    }

    @Transactional(readOnly = true)
    public UserDto currentUser(UUID userId) {
        return userRepository.findById(userId)
                .map(UserDto::from)
                .orElseThrow(ApiException::invalidCredentials);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefresh = generateRefreshToken();
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenTtlSeconds);
        refreshTokenRepository.save(new RefreshToken(sha256(rawRefresh), user, expiresAt));
        return new AuthResponse(
                accessToken, rawRefresh, jwtService.getAccessTokenTtlSeconds(), UserDto.from(user));
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nem elérhető", e);
        }
    }
}
