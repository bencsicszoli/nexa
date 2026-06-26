import { apiFetch } from '../lib/api'
import type { PublicUser } from './types'

// A backend /api/users végpontja (lásd com.nexa.user.UserController).

/** Egy felhasználó nyilvános profilja a hívóhoz viszonyított kapcsolatállapottal. */
export function getPublicProfile(userId: string): Promise<PublicUser> {
  return apiFetch<PublicUser>(`/users/${userId}`)
}
