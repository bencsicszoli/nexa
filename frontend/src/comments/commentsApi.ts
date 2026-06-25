import { apiFetch } from '../lib/api'
import type { Comment } from './types'

// A backend hozzászólás-végpontjai (lásd com.nexa.comment.CommentController).

/** Egy bejegyzés hozzászólás-fája (hozzászólások + beágyazott válaszok). */
export function getComments(postId: string): Promise<Comment[]> {
  return apiFetch<Comment[]>(`/posts/${postId}/comments`)
}

/** Új hozzászólás (parentId nélkül) vagy válasz (parentId egy meglévő kommentre). */
export function createComment(
  postId: string,
  content: string,
  parentId?: string,
): Promise<Comment> {
  return apiFetch<Comment>(`/posts/${postId}/comments`, {
    method: 'POST',
    body: parentId ? { content, parentId } : { content },
  })
}

/** Egy saját hozzászólás/válasz szövegének szerkesztése. */
export function updateComment(commentId: string, content: string): Promise<Comment> {
  return apiFetch<Comment>(`/comments/${commentId}`, { method: 'PATCH', body: { content } })
}

/** Egy hozzászólás/válasz törlése (szerző / posztoló / csoport-admin). */
export function deleteComment(commentId: string): Promise<void> {
  return apiFetch<void>(`/comments/${commentId}`, { method: 'DELETE' })
}
