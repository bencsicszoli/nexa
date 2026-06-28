package com.nexa.call;

/**
 * A WebRTC-jelzés (signaling) keretek típusai egy 1:1 videohíváshoz (#13). A backend csak
 * <b>relézi</b> ezeket a hívás két résztvevője között — a tényleges SDP/ICE-tartalmat nem
 * értelmezi (lásd {@link CallService}).
 *
 * <ul>
 *   <li>{@code OFFER} — a hívó ajánlata (SDP), egyben a hívás kezdeményezése (csörgés).</li>
 *   <li>{@code ANSWER} — a hívott válasza (SDP) elfogadáskor.</li>
 *   <li>{@code ICE} — ICE-jelölt csere mindkét irányban a kapcsolat felépítéséhez.</li>
 *   <li>{@code HANGUP} — a már élő hívás bontása bármelyik fél részéről.</li>
 *   <li>{@code REJECT} — a hívott elutasítja a bejövő hívást.</li>
 *   <li>{@code CANCEL} — a hívó visszavonja a még meg nem válaszolt hívást.</li>
 *   <li>{@code BUSY} — a hívott épp másik hívásban van.</li>
 * </ul>
 */
public enum CallSignalType {
    OFFER,
    ANSWER,
    ICE,
    HANGUP,
    REJECT,
    CANCEL,
    BUSY
}
