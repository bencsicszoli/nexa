// A backend /api/groups válaszainak típusai (lásd com.nexa.group.*).

/** A bejelentkezett felhasználó szerepe egy csoportban, vagy null, ha nem tagja. */
export type GroupRole = 'ADMIN' | 'MEMBER'

/** Csoport láthatósága / csatlakozási mód. */
export type GroupVisibility = 'PUBLIC' | 'PRIVATE'

/** Egy csoport a böngészéshez és a csoportoldalhoz. */
export type Group = {
  id: string
  name: string
  description: string | null
  /** A feltöltött logó URL-je; null esetén a frontend monogramot mutat. */
  logoUrl: string | null
  visibility: GroupVisibility
  memberCount: number
  /** A hívó szerepe; null = nem tag. */
  role: GroupRole | null
  /** Igaz, ha a hívónak függő csatlakozási kérelme van (privát csoport). */
  requested: boolean
  /** Függő kérelmek száma — csak adminnak releváns (egyébként 0). */
  pendingCount: number
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

/** Egy függő csatlakozási kérelem az admin jóváhagyó nézetéhez. */
export type GroupJoinRequest = {
  userId: string
  displayName: string
  avatarUrl: string | null
  requestedAt: string
}
