package com.nexa.group;

/**
 * Egy csoport láthatósága / csatlakozási módja (#9 kiegészítés).
 * <ul>
 *   <li>{@link #PUBLIC}: bárki azonnal csatlakozhat (taggá válik).</li>
 *   <li>{@link #PRIVATE}: a csatlakozás <b>kérelem</b> — az admin hagyja jóvá vagy utasítja el
 *       (lásd {@link GroupJoinRequest}).</li>
 * </ul>
 * Posztolni és kommentelni mindkét típusnál csak tag tud.
 */
public enum GroupVisibility {
    PUBLIC,
    PRIVATE
}
