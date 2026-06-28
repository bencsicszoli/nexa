package com.nexa.call.dto;

import java.util.List;

/**
 * A frontendnek visszaadott ICE-szerver lista a {@code RTCPeerConnection} konfigurációjához
 * (#13). A {@code iceServers} mező közvetlenül beadható a {@code new RTCPeerConnection({...})}
 * hívásnak. Egy STUN-bejegyzés (cím-felfedezés) és — ha be van állítva — egy TURN-bejegyzés
 * (relé szigorú NAT/tűzfal mögött) szerepel benne.
 *
 * @param iceServers a STUN/TURN szerverek (urls + opcionális username/credential a TURN-höz)
 */
public record IceServersDto(List<IceServer> iceServers) {

    /**
     * Egy ICE-szerver bejegyzés. A {@code username}/{@code credential} csak TURN-nél van kitöltve;
     * STUN-nál {@code null}, és a JSON-ból ilyenkor kimarad (lásd {@code @JsonInclude}).
     */
    public record IceServer(
            List<String> urls,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            String username,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            String credential) {

        public static IceServer stun(List<String> urls) {
            return new IceServer(urls, null, null);
        }

        public static IceServer turn(List<String> urls, String username, String credential) {
            return new IceServer(urls, username, credential);
        }
    }
}
