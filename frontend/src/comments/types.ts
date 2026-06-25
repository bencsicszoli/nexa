// A backend /api/.../comments válaszainak típusa (lásd com.nexa.comment.*).

/**
 * Egy hozzászólás vagy válasz a beágyazott válaszaival együtt (fa). A {@code parentId} null
 * a közvetlen hozzászólásnál; az {@code editedAt} nem null, ha a kommentet szerkesztették.
 */
export type Comment = {
  id: string
  postId: string
  parentId: string | null
  authorId: string
  authorName: string
  authorAvatarUrl: string | null
  content: string
  createdAt: string
  editedAt: string | null
  replies: Comment[]
}
