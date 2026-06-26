// A backend /api/users válaszának típusa (lásd com.nexa.user.dto.PublicUserDto).

import type { Relationship } from '../friends/types'

/** Egy felhasználó nyilvános profilja a hívóhoz viszonyított kapcsolatállapottal. */
export type PublicUser = {
  id: string
  displayName: string
  avatarUrl: string | null
  bio: string | null
  createdAt: string
  /** A hívó saját profilja-e (ekkor a kapcsolat-műveletek nem értelmezettek). */
  self: boolean
  /** Ismerősi állapot; selfnél 'SELF'. */
  friendStatus: Relationship | 'SELF'
  /** A függő ismerőskérés azonosítója (elfogadáshoz / visszavonáshoz), egyébként null. */
  friendRequestId: string | null
  /** Követi-e a hívó ezt a felhasználót. */
  following: boolean
}
