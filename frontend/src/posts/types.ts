// A backend /api/posts válaszainak típusa (lásd com.nexa.post.dto.PostDto).

export type MediaType = 'IMAGE' | 'VIDEO'

/** Egy a poszthoz csatolt média a megjelenítéshez. */
export type PostMedia = {
  url: string
  type: MediaType
  sizeBytes: number
}

export type Post = {
  id: string
  authorId: string
  authorName: string
  authorAvatarUrl: string | null
  content: string
  media: PostMedia[]
  createdAt: string
}
