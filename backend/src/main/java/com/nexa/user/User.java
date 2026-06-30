package com.nexa.user;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

/**
 * Regisztrált felhasználó. Az érzékeny adat (e-mail, jelszó-hash) ide,
 * a PostgreSQL {@code users} táblába kerül (lásd CLAUDE.md adatbázis-elv).
 * A jelszót sosem tároljuk nyíltan — csak BCrypt-hash formájában.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Rövid bemutatkozás (opcionális). A #4 kártya vezeti be. */
    @Column(name = "bio", length = 280)
    private String bio;

    /** Az avatar publikus URL-je (opcionális); a kép az objektumtárolóba kerül. */
    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    /**
     * A felület nyelve a felhasználónak (#17). A bejelentkezéskor/{@code /auth/me}-kor a frontend
     * ezt állítja be. DB-default 'hu', hogy a séma-frissítéskor a meglévő sorok is értéket kapjanak.
     */
    @Column(name = "locale", nullable = false, length = 8)
    @ColumnDefault("'hu'")
    private String locale = "hu";

    /** Megjelenhet-e a felhasználó a keresőben/felfedezésben (#17). */
    @Column(name = "searchable", nullable = false)
    @ColumnDefault("true")
    private boolean searchable = true;

    /** Elrejtse-e az online jelenlétét mások elől (#17). */
    @Column(name = "hide_presence", nullable = false)
    @ColumnDefault("false")
    private boolean hidePresence = false;

    /** Be van-e kapcsolva a kétlépcsős hitelesítés (TOTP, #17); alapból ki. */
    @Column(name = "totp_enabled", nullable = false)
    @ColumnDefault("false")
    private boolean totpEnabled = false;

    /**
     * A TOTP titok (Base32), AES-GCM-mel <b>titkosítva</b> tárolva (lásd {@code SecretCipher}).
     * {@code null}, ha a 2FA nincs beállítva.
     */
    @Column(name = "totp_secret")
    private String totpSecret;

    /** Értesítési preferenciák típusonként, JSON-ként tárolva (#17). Null → {@link NotificationPrefs#defaults()}. */
    @Convert(converter = NotificationPrefsConverter.class)
    @Column(name = "notification_prefs", columnDefinition = "text")
    private NotificationPrefs notificationPrefs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {
        // JPA
    }

    public User(String email, String displayName, String passwordHash) {
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public boolean isHidePresence() {
        return hidePresence;
    }

    public void setHidePresence(boolean hidePresence) {
        this.hidePresence = hidePresence;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    /** Az értesítési preferenciák; ha még nincs beállítva (null), a mindent engedő alapértelmezés. */
    public NotificationPrefs getNotificationPrefs() {
        return notificationPrefs == null ? NotificationPrefs.defaults() : notificationPrefs;
    }

    public void setNotificationPrefs(NotificationPrefs notificationPrefs) {
        this.notificationPrefs = notificationPrefs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
