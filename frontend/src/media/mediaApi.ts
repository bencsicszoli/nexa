import { apiFetch } from '../lib/api'
import type { MediaType } from '../posts/types'
import type { MediaItem } from './types'

// A backend /api/library végpontjai (lásd com.nexa.media.MediaLibraryController).

/** A tárolótól kapott aláírt feltöltési cél. */
export type MediaUploadTarget = {
  uploadUrl: string
  key: string
}

/** Aláírt feltöltési cél kérése a médiatárba kerülő kép/videó számára. */
export function requestLibraryUploadUrl(contentType: string): Promise<MediaUploadTarget> {
  return apiFetch<MediaUploadTarget>('/library/upload-url', {
    method: 'POST',
    body: { contentType },
  })
}

/**
 * A fájl feltöltése közvetlenül az aláírt URL-re (nem az apiFetch-en: nincs JWT, nincs JSON).
 * A {@code contentType} explicit — egyes formátumoknál (pl. .mkv) a böngésző üres {@code file.type}-ot ad.
 */
export async function uploadLibraryFile(
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

/** Egy feltöltött elem megerősítése (perzisztálás a médiatárba). */
export function confirmLibraryMedia(
  key: string,
  type: MediaType,
  sizeBytes: number,
): Promise<MediaItem> {
  return apiFetch<MediaItem>('/library', { method: 'POST', body: { key, type, sizeBytes } })
}

/** A bejelentkezett felhasználó médiatára, legfrissebb felül. */
export function getLibrary(): Promise<MediaItem[]> {
  return apiFetch<MediaItem[]>('/library')
}

/** Egy saját médiatár-elem törlése (a fájl is törlődik). */
export function deleteLibraryItem(id: string): Promise<void> {
  return apiFetch<void>(`/library/${id}`, { method: 'DELETE' })
}
