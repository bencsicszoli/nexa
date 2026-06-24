import { apiFetch } from '../lib/api'
import { clearTokens, getRefreshToken, setTokens } from './tokenStore'
import type { AuthResponse, User } from './types'

export async function registerRequest(
  email: string,
  displayName: string,
  password: string,
): Promise<AuthResponse> {
  const res = await apiFetch<AuthResponse>('/auth/register', {
    method: 'POST',
    auth: false,
    body: { email, displayName, password },
  })
  setTokens(res.accessToken, res.refreshToken)
  return res
}

export async function loginRequest(email: string, password: string): Promise<AuthResponse> {
  const res = await apiFetch<AuthResponse>('/auth/login', {
    method: 'POST',
    auth: false,
    body: { email, password },
  })
  setTokens(res.accessToken, res.refreshToken)
  return res
}

/** Szerveroldali kijelentkezés (refresh token visszavonása), majd helyi tokentörlés. */
export async function logoutRequest(): Promise<void> {
  const refreshToken = getRefreshToken()
  try {
    await apiFetch<void>('/auth/logout', {
      method: 'POST',
      auth: false,
      body: { refreshToken: refreshToken ?? '' },
    })
  } catch {
    // A kijelentkezés a kliensoldalon akkor is sikerül, ha a hálózati hívás elbukik.
  } finally {
    clearTokens()
  }
}

export function fetchMe(): Promise<User> {
  return apiFetch<User>('/auth/me')
}
