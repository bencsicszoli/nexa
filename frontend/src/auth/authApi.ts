import { apiFetch } from '../lib/api'
import { clearTokens, getRefreshToken, setTokens } from './tokenStore'
import type { AuthResponse, LoginResult, User } from './types'

/** A 2FA-s login köztes válasza a backendtől (lásd AuthController.login). */
type LoginResponse = AuthResponse & { twoFactorRequired?: boolean; challengeToken?: string }

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

/**
 * Bejelentkezés. 2FA nélkül beállítja a tokeneket és a felhasználót adja vissza; ha a 2FA be van
 * kapcsolva, NEM állít tokent, hanem a challenge tokent adja vissza (a kódot a {@link loginWith2faRequest}
 * váltja tokenre).
 */
export async function loginRequest(email: string, password: string): Promise<LoginResult> {
  const res = await apiFetch<LoginResponse>('/auth/login', {
    method: 'POST',
    auth: false,
    body: { email, password },
  })
  if (res.twoFactorRequired && res.challengeToken) {
    return { kind: '2fa', challengeToken: res.challengeToken }
  }
  setTokens(res.accessToken, res.refreshToken)
  return { kind: 'authenticated', user: res.user }
}

/** A kétlépcsős login második lépése: challenge token + 2FA kód → token-páros + felhasználó. */
export async function loginWith2faRequest(
  challengeToken: string,
  code: string,
): Promise<User> {
  const res = await apiFetch<AuthResponse>('/auth/login/2fa', {
    method: 'POST',
    auth: false,
    body: { challengeToken, code },
  })
  setTokens(res.accessToken, res.refreshToken)
  return res.user
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
