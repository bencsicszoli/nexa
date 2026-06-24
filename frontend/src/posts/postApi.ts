import { apiFetch } from '../lib/api'
import type { Post } from './types'

// A backend /api/posts végpontjai (lásd com.nexa.post.PostController).

/** Új szöveges bejegyzés létrehozása. */
export function createPost(content: string): Promise<Post> {
  return apiFetch<Post>('/posts', { method: 'POST', body: { content } })
}

/** A bejelentkezett felhasználó saját bejegyzései, legfrissebb felül. */
export function getMyPosts(): Promise<Post[]> {
  return apiFetch<Post[]>('/posts/me')
}
