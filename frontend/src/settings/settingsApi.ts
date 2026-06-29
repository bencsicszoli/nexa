import { apiFetch } from '../lib/api'

/** Az értesítési preferenciák típusonként (lásd com.nexa.user.NotificationPrefs). */
export type NotificationPrefs = {
  newPost: boolean
  friendRequest: boolean
  friendAccepted: boolean
  newFollower: boolean
}

/** A beállítások teljes állapota (lásd com.nexa.settings.dto.SettingsDto). */
export type Settings = {
  locale: string
  searchable: boolean
  hidePresence: boolean
  notificationPrefs: NotificationPrefs
  totpEnabled: boolean
}

export type TwoFactorSetup = { secret: string; otpauthUri: string }
export type RecoveryCodes = { recoveryCodes: string[] }

export function getSettings(): Promise<Settings> {
  return apiFetch<Settings>('/settings')
}

export function updateLocale(locale: string): Promise<Settings> {
  return apiFetch<Settings>('/settings/locale', { method: 'PATCH', body: { locale } })
}

export function updateNotificationPrefs(prefs: NotificationPrefs): Promise<Settings> {
  return apiFetch<Settings>('/settings/notifications', { method: 'PATCH', body: prefs })
}

export function updatePrivacy(searchable: boolean, hidePresence: boolean): Promise<Settings> {
  return apiFetch<Settings>('/settings/privacy', {
    method: 'PATCH',
    body: { searchable, hidePresence },
  })
}

export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return apiFetch<void>('/settings/password', {
    method: 'POST',
    body: { currentPassword, newPassword },
  })
}

// --- 2FA ---

export function begin2faSetup(): Promise<TwoFactorSetup> {
  return apiFetch<TwoFactorSetup>('/auth/2fa/setup', { method: 'POST' })
}

export function enable2fa(code: string): Promise<RecoveryCodes> {
  return apiFetch<RecoveryCodes>('/auth/2fa/enable', { method: 'POST', body: { code } })
}

export function disable2fa(code: string): Promise<void> {
  return apiFetch<void>('/auth/2fa/disable', { method: 'POST', body: { code } })
}
