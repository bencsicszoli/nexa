package com.nexa.auth.dto;

/**
 * Sikeres regisztráció/bejelentkezés/frissítés válasza.
 * Az {@code accessToken}-t a frontend a memóriában/localStorage-ban tartja és
 * a {@code Authorization: Bearer} fejlécben küldi; a {@code refreshToken} a csendes
 * megújításhoz kell. (Hardening — httpOnly cookie — a #18 kártya feladata.)
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserDto user) {
}
