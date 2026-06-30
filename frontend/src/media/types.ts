// A backend /api/library válaszainak típusa (lásd com.nexa.media.dto.MediaItemDto).

import type { MediaType } from '../posts/types'

/** A személyes médiatár egy eleme (a bejegyzésektől független, közvetlenül feltöltött kép/videó). */
export type MediaItem = {
  id: string
  url: string
  type: MediaType
  sizeBytes: number
  createdAt: string
}
