package com.nexa.auth;

import com.nexa.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Egy egyszer-használatos 2FA helyreállító kód (#17). A kódot sosem tároljuk nyíltan, csak a
 * BCrypt-hash-ét — ugyanaz az elv, mint a jelszónál. A 2FA bekapcsolásakor generálódik egy köteg,
 * a felhasználó egyszer látja nyersen; beváltáskor a sor {@code used=true} lesz.
 */
@Entity
@Table(name = "recovery_codes", indexes = {
        @Index(name = "idx_recovery_codes_user", columnList = "user_id, used")
})
public class RecoveryCode {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected RecoveryCode() {
        // JPA
    }

    public RecoveryCode(User user, String codeHash) {
        this.user = user;
        this.codeHash = codeHash;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public boolean isUsed() {
        return used;
    }

    public void markUsed() {
        this.used = true;
    }
}
