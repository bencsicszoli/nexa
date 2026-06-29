package com.nexa.auth.dto;

/**
 * A bejelentkezés kétféle kimenete (#17): vagy kész token-páros ({@code auth}), vagy — ha a
 * felhasználónál be van kapcsolva a 2FA — egy {@code challengeToken}, amivel a {@code /login/2fa}
 * a 6 jegyű kód megadása után állítja ki a tokeneket. A {@code controller} ennek alapján vagy az
 * {@link AuthResponse}-t, vagy egy {@code {twoFactorRequired, challengeToken}} választ küld.
 */
public record LoginResult(boolean twoFactorRequired, String challengeToken, AuthResponse auth) {

    public static LoginResult authenticated(AuthResponse auth) {
        return new LoginResult(false, null, auth);
    }

    public static LoginResult challenge(String challengeToken) {
        return new LoginResult(true, challengeToken, null);
    }
}
