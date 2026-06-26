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

/** Egy adott felhasználó bejegyzései (más profiljának megtekintéséhez), legfrissebb felül. */
export function getUserPosts(userId: string): Promise<Post[]> {
  return apiFetch<Post[]>(`/posts/user/${userId}`)
}

/** Egy saját bejegyzés szövegének szerkesztése (a média változatlan). */
export function updatePost(id: string, content: string): Promise<Post> {
  return apiFetch<Post>(`/posts/${id}`, { method: 'PATCH', body: { content } })
}

/** Egy saját bejegyzés törlése. */
export function deletePost(id: string): Promise<void> {
  return apiFetch<void>(`/posts/${id}`, { method: 'DELETE' })
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
 * objektumtároló). Nem az apiFetch-en megy: nincs JWT és nincs JSON. A {@code contentType}
 * explicit, mert egyes formátumoknál (pl. .mkv) a böngésző üres {@code file.type}-ot ad,
 * és a háttér ezt veti össze az aláírt típussal.
 */
export async function uploadPostMediaFile(
  uploadUrl: string,
  file: File,
  contentType: string,
): Promise<void> {
  const res = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': contentType },
    body: file,
  })
  if (!res.ok) throw new Error(`Media upload failed: ${res.status}`)
}
