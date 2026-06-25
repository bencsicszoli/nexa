// A backend /api/friends válaszainak típusai (lásd com.nexa.friend.*).

/** Egy elfogadott ismerős a listához. */
export type Friend = {
  id: string
  displayName: string
  avatarUrl: string | null
  bio: string | null
  friendsSince: string
}

/** Egy függőben lévő ismerőskérés (beérkezett vagy elküldött). */
export type FriendRequest = {
  requestId: string
  userId: string
  displayName: string
  avatarUrl: string | null
  bio: string | null
  createdAt: string
}

export type FriendRequests = {
  incoming: FriendRequest[]
  outgoing: FriendRequest[]
}

/** A bejelentkezett felhasználóhoz viszonyított kapcsolatállapot az „Emberek" böngészésénél. */
export type Relationship = 'NONE' | 'FRIENDS' | 'REQUEST_SENT' | 'REQUEST_RECEIVED'

/** Egy böngészett felhasználó a kapcsolatállapottal. */
export type PersonSummary = {
  id: string
  displayName: string
  avatarUrl: string | null
  bio: string | null
  relationship: Relationship
  /** Csak függő kérésnél (SENT/RECEIVED) van kitöltve — elfogadáshoz / visszavonáshoz. */
  requestId: string | null
}
