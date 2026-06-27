package com.nexa.chat;

/**
 * A beszélgetés fajtája (#12).
 * <ul>
 *   <li>{@link #DIRECT} — kétszemélyes (1:1) csevegés; a résztvevők a
 *       {@link ConversationParticipant} sorokban élnek.</li>
 *   <li>{@link #GROUP} — egy {@link com.nexa.group.Group csoporthoz} kötött csevegés;
 *       a résztvevők a csoport aktuális tagjai (külön résztvevő-sorok nélkül).</li>
 * </ul>
 */
public enum ConversationType {
    DIRECT,
    GROUP
}
