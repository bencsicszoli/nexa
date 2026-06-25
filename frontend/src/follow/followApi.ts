import { apiFetch } from '../lib/api'
import type { FollowUser } from './types'

// A backend /api/follows végpontjai (lásd com.nexa.follow.FollowController).

/** Akiket a bejelentkezett felhasználó követ. */
export function getFollowing(): Promise<FollowUser[]> {
  return apiFetch<FollowUser[]>('/follows/following')
}

/** Akik a bejelentkezett felhasználót követik. */
export function getFollowers(): Promise<FollowUser[]> {
  return apiFetch<FollowUser[]>('/follows/followers')
}

/** Egy felhasználó követése (idempotens). */
export function followUser(userId: string): Promise<void> {
  return apiFetch<void>(`/follows/${userId}`, { method: 'PUT' })
}

/** Egy felhasználó lekövetése (idempotens). */
export function unfollowUser(userId: string): Promise<void> {
  return apiFetch<void>(`/follows/${userId}`, { method: 'DELETE' })
}
