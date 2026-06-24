import { apiFetch } from '../lib/api'
import type { MediaType, Post } from './types'

// A backend /api/posts végpontjai (lásd com.nexa.post.PostController).

/** A tárolótól kapott aláírt média-feltöltési cél. */
export type MediaUploadTarget = {
  uploadUrl: string
  key: string
}

/** Egy poszthoz csatolandó, már feltöltött média hivatkozása a létrehozásnál. */
export type PostMediaInput = {
  key: string
  type: MediaType
  sizeBytes: number
}

/** Új bejegyzés létrehozása szöveggel és/vagy feltöltött médiával. */
export function createPost(content: string, media: PostMediaInput[] = []): Promise<Post> {
  return apiFetch<Post>('/posts', { method: 'POST', body: { content, media } })
}

/** A bejelentkezett felhasználó saját bejegyzései, legfrissebb felül. */
export function getMyPosts(): Promise<Post[]> {
  return apiFetch<Post[]>('/posts/me')
}

/** Aláírt feltöltési cél kérése egy poszthoz csatolandó kép/videó számára. */
export function requestPostMediaUploadUrl(contentType: string): Promise<MediaUploadTarget> {
  return apiFetch<MediaUploadTarget>('/posts/media/upload-url', {
    method: 'POST',
    body: { contentType },
  })
}

/**
 * A fájl feltöltése közvetlenül az aláírt URL-re (lokálnál a backend, R2-nél az
 * objektumtároló). Nem az apiFetch-en megy: nincs JWT és nincs JSON.
 */
export async function uploadPostMediaFile(uploadUrl: string, file: File): Promise<void> {
  const res = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type },
    body: file,
  })
  if (!res.ok) throw new Error(`Media upload failed: ${res.status}`)
}
