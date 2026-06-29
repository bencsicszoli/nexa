// A backend /api/search válaszának típusai (lásd com.nexa.search.*).

import type { Group } from '../groups/types'
import type { Post } from '../posts/types'

/** Egy felhasználó-találat a keresőben (kapcsolatállapot nélkül; azt a profiloldal tölti be). */
export type SearchUser = {
  id: string
  displayName: string
  avatarUrl: string | null
  bio: string | null
}

/** A keresés aggregált találatai mindhárom típusra. */
export type SearchResults = {
  users: SearchUser[]
  groups: Group[]
  posts: Post[]
}
