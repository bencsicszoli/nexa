import { apiFetch } from '../lib/api'
import type { User } from '../auth/types'

// A backend /api/profile végpontjai (lásd com.nexa.profile.ProfileController).

/** A tárolótól kapott aláírt feltöltési cél. */
export type UploadTarget = {
  uploadUrl: string
  key: string
}

export function getProfile(): Promise<User> {
  return apiFetch<User>('/profile')
}

export function updateProfile(displayName: string, bio: string): Promise<User> {
  return apiFetch<User>('/profile', { method: 'PATCH', body: { displayName, bio } })
}

export function requestAvatarUploadUrl(contentType: string): Promise<UploadTarget> {
  return apiFetch<UploadTarget>('/profile/avatar/upload-url', {
    method: 'POST',
    body: { contentType },
  })
}

/**
 * A fájl feltöltése közvetlenül az aláírt URL-re (lokálnál a backend, R2-nél az
 * objektumtároló). Nem az apiFetch-en megy: nincs JWT és nincs JSON.
 */
export async function uploadImageFile(uploadUrl: string, file: File): Promise<void> {
  const res = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type },
    body: file,
  })
  if (!res.ok) throw new Error(`Image upload failed: ${res.status}`)
}

export function confirmAvatar(key: string): Promise<User> {
  return apiFetch<User>('/profile/avatar', { method: 'PUT', body: { key } })
}

export function removeAvatar(): Promise<User> {
  return apiFetch<User>('/profile/avatar', { method: 'DELETE' })
}

// Borítókép (3:1) — az avatar-végpontok tükre (lásd com.nexa.profile.ProfileController).

export function requestCoverUploadUrl(contentType: string): Promise<UploadTarget> {
  return apiFetch<UploadTarget>('/profile/cover/upload-url', {
    method: 'POST',
    body: { contentType },
  })
}

export function confirmCover(key: string): Promise<User> {
  return apiFetch<User>('/profile/cover', { method: 'PUT', body: { key } })
}

export function removeCover(): Promise<User> {
  return apiFetch<User>('/profile/cover', { method: 'DELETE' })
}
