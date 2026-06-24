package com.nexa.auth;

import com.nexa.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    void deleteByTokenHash(String tokenHash);

    @Transactional
    @Modifying
    void deleteByUser(User user);

    @Transactional
    @Modifying
    @Query("delete from RefreshToken rt where rt.expiresAt < :now")
    int deleteExpired(Instant now);
}
