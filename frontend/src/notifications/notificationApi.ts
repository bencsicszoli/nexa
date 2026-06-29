import { apiFetch } from '../lib/api'
import type { NexaNotification } from './types'

/** Az értesítés-előzmény egy lapja (lásd com.nexa.realtime.dto.NotificationPageDto). */
export type NotificationPage = {
  items: NexaNotification[]
  page: number
  hasMore: boolean
}

/** Az előzmény egy lapja, legfrissebb felül. */
export function getNotifications(page = 0, size = 20): Promise<NotificationPage> {
  return apiFetch<NotificationPage>(`/notifications?page=${page}&size=${size}`)
}

/** Az olvasatlan értesítések száma a harang-jelvényhez. */
export function getUnreadCount(): Promise<number> {
  return apiFetch<{ count: number }>('/notifications/unread-count').then((r) => r.count)
}

/** Minden értesítés olvasottra állítása. */
export function markAllRead(): Promise<void> {
  return apiFetch<void>('/notifications/read-all', { method: 'POST' })
}

/** Egy értesítés olvasottra állítása. */
export function markRead(id: string): Promise<void> {
  return apiFetch<void>(`/notifications/${id}/read`, { method: 'POST' })
}
