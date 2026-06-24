// A backend /api/posts válaszainak típusa (lásd com.nexa.post.dto.PostDto).

export type Post = {
  id: string
  authorId: string
  authorName: string
  authorAvatarUrl: string | null
  content: string
  createdAt: string
}
