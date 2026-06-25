// A backend /api/groups válaszainak típusai (lásd com.nexa.group.*).

/** A bejelentkezett felhasználó szerepe egy csoportban, vagy null, ha nem tagja. */
export type GroupRole = 'ADMIN' | 'MEMBER'

/** Egy csoport a böngészéshez és a csoportoldalhoz. */
export type Group = {
  id: string
  name: string
  description: string | null
  memberCount: number
  /** A hívó szerepe; null = nem tag (a UI „Csatlakozás"-t ajánl). */
  role: GroupRole | null
  createdAt: string
}

/** Egy csoporttag a tagok listájához. */
export type GroupMember = {
  id: string
  displayName: string
  avatarUrl: string | null
  role: GroupRole
  joinedAt: string
}
