package com.nexa.security;

import com.nexa.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Rövid életű access JWT kiállítása és ellenőrzése (HS256).
 * A refresh tokent NEM ez kezeli — az opak, DB-ben tárolt (lásd {@link com.nexa.auth.RefreshToken}).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenTtlSeconds;

    public JwtService(
            @Value("${nexa.jwt.secret}") String secret,
            @Value("${nexa.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "A nexa.jwt.secret legalább 32 bájt (256 bit) legyen a HS256-hoz.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    /** A kétlépcsős login köztes (challenge) tokenjének típusjelzője és élettartama (#17). */
    private static final String CHALLENGE_TYPE = "2fa_challenge";
    private static final long CHALLENGE_TTL_SECONDS = 300; // 5 perc

    /** Access token a felhasználónak: subject = userId, plusz email és role claim. */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * Rövid életű challenge token a kétlépcsős loginhoz (#17): a jelszó már stimmelt, de a
     * 2FA kód még hátravan. A {@code typ=2fa_challenge} claim miatt ez NEM használható access
     * tokenként (lásd {@link #extractUserId} és a {@code JwtAuthenticationFilter}).
     */
    public String generate2faChallengeToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("typ", CHALLENGE_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(CHALLENGE_TTL_SECONDS)))
                .signWith(key)
                .compact();
    }

    /** A challenge tokenből kinyert userId; kivétel, ha nem challenge típusú, érvénytelen vagy lejárt. */
    public UUID extract2faChallengeUserId(String token) {
        Claims claims = parse(token);
        if (!CHALLENGE_TYPE.equals(claims.get("typ", String.class))) {
            throw new IllegalArgumentException("Not a 2FA challenge token");
        }
        return UUID.fromString(claims.getSubject());
    }

    /**
     * A (access) tokenből kinyert felhasználó-azonosító, vagy kivétel, ha érvénytelen/lejárt.
     * A {@code 2fa_challenge} típusú tokent <b>elutasítja</b> — az csak a {@code /login/2fa}-ra jó.
     */
    public UUID extractUserId(String token) {
        Claims claims = parse(token);
        if (CHALLENGE_TYPE.equals(claims.get("typ", String.class))) {
            throw new IllegalArgumentException("2FA challenge token is not a valid access token");
        }
        return UUID.fromString(claims.getSubject());
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
