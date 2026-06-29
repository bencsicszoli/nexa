package com.nexa.realtime;

/**
 * A perzisztált értesítések típusai (#17). Az érték a {@link com.nexa.user.NotificationPrefs}
 * megfelelő kapcsolójához kötődik, és a frontend ennek alapján rajzol szöveget/navigációt.
 */
public enum NotificationType {
    /** Egy kapcsolat új bejegyzést tett közzé (a hírfolyamban megjelenne). */
    NEW_POST,
    /** Valaki ismerőskérést küldött. */
    FRIEND_REQUEST,
    /** Egy elküldött ismerőskérést elfogadtak. */
    FRIEND_ACCEPTED,
    /** Valaki követni kezdte a felhasználót. */
    NEW_FOLLOWER
}
