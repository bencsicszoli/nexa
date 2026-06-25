package com.nexa.friend;

/**
 * Egy ismerősi kapcsolat állapota. Kétirányú kapcsolat: a kezdeményező (requester)
 * küld kérést a címzettnek (addressee).
 * <ul>
 *     <li>{@code PENDING} — elküldött, még el nem fogadott kérés.</li>
 *     <li>{@code ACCEPTED} — kölcsönös ismerősi kapcsolat.</li>
 * </ul>
 * Elutasításkor / visszavonáskor a rekord törlődik (nem külön állapot), így a pár
 * között később újra lehet kérést küldeni.
 */
public enum FriendshipStatus {
    PENDING,
    ACCEPTED
}
