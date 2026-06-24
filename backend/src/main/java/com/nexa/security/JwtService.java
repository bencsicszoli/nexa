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

    /** A tokenből kinyert felhasználó-azonosító, vagy kivétel, ha érvénytelen/lejárt. */
    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
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
