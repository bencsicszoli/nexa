package com.nexa.realtime;

import java.security.Principal;

/**
 * A STOMP-munkamenethez rendelt felhasználó-azonosító mint {@link Principal}.
 * A {@code name} a felhasználó UUID-ja szövegként; erre címezünk a
 * {@code convertAndSendToUser(userId, ...)} hívásokkal (a kliens a
 * {@code /user/queue/notifications} célra iratkozik fel).
 */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
