// A backend /api/follows válaszainak típusai (lásd com.nexa.follow.*).

/** Egy követett vagy követő felhasználó a listához. */
export type FollowUser = {
  id: string
  displayName: string
  avatarUrl: string | null
  bio: string | null
  since: string
}
