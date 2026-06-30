// A backend /api/posts válaszainak típusa (lásd com.nexa.post.dto.PostDto).

export type MediaType = 'IMAGE' | 'VIDEO'

/** Egy a poszthoz csatolt média a megjelenítéshez. */
export type PostMedia = {
  url: string
  type: MediaType
  sizeBytes: number
}

/** A bejegyzés forráscsoportja (csak csoportposztnál) — a hírfolyam-jelöléshez. */
export type PostGroup = {
  id: string
  name: string
  logoUrl: string | null
}

export type Post = {
  id: string
  authorId: string
  authorName: string
  authorAvatarUrl: string | null
  content: string
  media: PostMedia[]
  /** Kitöltve, ha a bejegyzés egy csoporthoz tartozik; profilposztnál null. */
  group: PostGroup | null
  createdAt: string
  /** Kitöltve, ha a bejegyzést szerkesztették; null ha még sohasem módosult. */
  editedAt: string | null
}
