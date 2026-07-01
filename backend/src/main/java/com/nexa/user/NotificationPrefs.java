package com.nexa.user;

/**
 * A felhasználó értesítési preferenciái típusonként (#17). A {@link com.nexa.user.User}-en
 * JSON-ként tárolva (lásd {@link NotificationPrefsConverter}). Egy típus kikapcsolása esetén
 * az adott esemény sem nem perzisztálódik, sem nem megy ki push-ban a felhasználónak.
 *
 * <p>A {@link #defaults()} mindent bekapcsol — ez az alapértelmezés a friss/null prefű
 * felhasználóknak is (lásd {@link User#getNotificationPrefs()}).
 */
public record NotificationPrefs(
        boolean newPost,
        boolean friendRequest,
        boolean friendAccepted,
        boolean newFollower,
        boolean groupJoinRequest) {

    /** Minden értesítéstípus bekapcsolva — az alapértelmezett beállítás. */
    public static NotificationPrefs defaults() {
        return new NotificationPrefs(true, true, true, true, true);
    }
}
