// Az 1:1 videohívás (#13) kliensoldali típusai. A jelzés (signaling) a megosztott STOMP-kapcsolaton
// megy (lásd com.nexa.call.*): a kliens a /app/call.signal célra küld, és a /user/queue/call címen kap.

/** A WebRTC-jelzés keretek típusai (megegyezik a backend CallSignalType-pal). */
export type CallSignalType =
  | 'OFFER'
  | 'ANSWER'
  | 'ICE'
  | 'HANGUP'
  | 'REJECT'
  | 'CANCEL'
  | 'BUSY'

/** A másik félnek küldött / tőle kapott jelzés. Az sdp/candidate a böngésző natív objektuma. */
export type CallSignal = {
  conversationId: string
  fromUserId: string
  fromName: string
  fromAvatarUrl: string | null
  type: CallSignalType
  sdp: RTCSessionDescriptionInit | null
  candidate: RTCIceCandidateInit | null
}

/** A hívás állapotgépe. */
export type CallStatus =
  /** Nincs hívás. */
  | 'idle'
  /** Mi hívunk valakit, csörög nála (még nincs válasz). */
  | 'outgoing'
  /** Valaki minket hív, dönthetünk az elfogadásról. */
  | 'incoming'
  /** Élő (vagy épp felépülő) hívás. */
  | 'active'

/** A hívásban részt vevő másik fél megjelenítendő adatai. */
export type CallPeer = {
  userId: string
  name: string
  avatarUrl: string | null
}
