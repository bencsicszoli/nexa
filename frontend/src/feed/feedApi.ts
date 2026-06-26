import { apiFetch } from '../lib/api'
import type { Post } from '../posts/types'

// A backend /api/feed végpontja (lásd com.nexa.feed.FeedController).
// Az algoritmusmentes, időrendi hírfolyam: ismerősök + követettek + tag-csoportok posztjai.

/** A hírfolyam egy lapja: a bejegyzések és a következő laphoz tartozó átlátszatlan cursor. */
export type FeedPage = {
  items: Post[]
  /** A következő lap cursora, vagy null, ha nincs több bejegyzés. */
  nextCursor: string | null
}

/**
 * A hírfolyam egy lapja. {@code cursor} elhagyva az első lapot adja; egyébként egy
 * korábbi lap {@code nextCursor}-át kell visszaküldeni a következőért (keyset-lapozás).
 */
export function getFeed(cursor?: string | null, limit?: number): Promise<FeedPage> {
  const params = new URLSearchParams()
  if (cursor) params.set('cursor', cursor)
  if (limit) params.set('limit', String(limit))
  const qs = params.toString()
  return apiFetch<FeedPage>(`/feed${qs ? `?${qs}` : ''}`)
}
