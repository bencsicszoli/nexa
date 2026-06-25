// A backend /api/.../comments válaszainak típusa (lásd com.nexa.comment.*).

import type { PostMedia } from '../posts/types'

/**
 * Egy hozzászólás vagy válasz a beágyazott válaszaival együtt (fa). A {@code parentId} null
 * a közvetlen hozzászólásnál; az {@code editedAt} nem null, ha a kommentet szerkesztették.
 * A {@code media} a csatolt képek/videók (a PostMedia-val azonos szerkezet).
 */
export type Comment = {
  id: string
  postId: string
  parentId: string | null
  authorId: string
  authorName: string
  authorAvatarUrl: string | null
  content: string
  media: PostMedia[]
  createdAt: string
  editedAt: string | null
  replies: Comment[]
}
