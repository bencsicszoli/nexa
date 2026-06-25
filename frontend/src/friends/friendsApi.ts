import { apiFetch } from '../lib/api'
import type { Friend, FriendRequests, PersonSummary } from './types'

// A backend /api/friends végpontjai (lásd com.nexa.friend.FriendController).

/** A bejelentkezett felhasználó elfogadott ismerősei. */
export function getFriends(): Promise<Friend[]> {
  return apiFetch<Friend[]>('/friends')
}

/** Függőben lévő kérések: beérkezett (elfogadható) + elküldött (visszavonható). */
export function getRequests(): Promise<FriendRequests> {
  return apiFetch<FriendRequests>('/friends/requests')
}

/** Felhasználók böngészése a kapcsolatállapottal (kérésküldéshez). */
export function browsePeople(query: string): Promise<PersonSummary[]> {
  const q = query.trim()
  const suffix = q ? `?query=${encodeURIComponent(q)}` : ''
  return apiFetch<PersonSummary[]>(`/friends/people${suffix}`)
}

/** Ismerőskérés küldése egy másik felhasználónak. */
export function sendFriendRequest(userId: string): Promise<void> {
  return apiFetch<void>('/friends/requests', { method: 'POST', body: { userId } })
}

/** Egy beérkezett kérés elfogadása. */
export function acceptRequest(requestId: string): Promise<void> {
  return apiFetch<void>(`/friends/requests/${requestId}/accept`, { method: 'POST' })
}

/** Egy függő kérés elutasítása (címzettként) vagy visszavonása (kezdeményezőként). */
export function removeRequest(requestId: string): Promise<void> {
  return apiFetch<void>(`/friends/requests/${requestId}`, { method: 'DELETE' })
}

/** Egy ismerős eltávolítása. */
export function removeFriend(userId: string): Promise<void> {
  return apiFetch<void>(`/friends/${userId}`, { method: 'DELETE' })
}
