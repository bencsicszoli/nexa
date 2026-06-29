package com.nexa.auth;

import com.nexa.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, UUID> {

    /** A felhasználó még fel nem használt helyreállító kódjai (beváltáskor ezekkel illesztünk). */
    List<RecoveryCode> findByUserIdAndUsedFalse(UUID userId);

    @Transactional
    @Modifying
    void deleteByUser(User user);
}
