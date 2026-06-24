import { apiFetch } from '../lib/api'
import type { User } from '../auth/types'

// A backend /api/profile végpontjai (lásd com.nexa.profile.ProfileController).

/** A tárolótól kapott aláírt feltöltési cél. */
export type AvatarUploadTarget = {
  uploadUrl: string
  key: string
}

export function getProfile(): Promise<User> {
  return apiFetch<User>('/profile')
}

export function updateProfile(displayName: string, bio: string): Promise<User> {
  return apiFetch<User>('/profile', { method: 'PATCH', body: { displayName, bio } })
}

export function requestAvatarUploadUrl(contentType: string): Promise<AvatarUploadTarget> {
  return apiFetch<AvatarUploadTarget>('/profile/avatar/upload-url', {
    method: 'POST',
    body: { contentType },
  })
}

/**
 * A fájl feltöltése közvetlenül az aláírt URL-re (lokálnál a backend, R2-nél az
 * objektumtároló). Nem az apiFetch-en megy: nincs JWT és nincs JSON.
 */
export async function uploadAvatarFile(uploadUrl: string, file: File): Promise<void> {
  const res = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type },
    body: file,
  })
  if (!res.ok) throw new Error(`Avatar upload failed: ${res.status}`)
}

export function confirmAvatar(key: string): Promise<User> {
  return apiFetch<User>('/profile/avatar', { method: 'PUT', body: { key } })
}

export function removeAvatar(): Promise<User> {
  return apiFetch<User>('/profile/avatar', { method: 'DELETE' })
}
