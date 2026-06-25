import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setTokens,
} from '../auth/tokenStore'
import type { ApiErrorBody, AuthResponse } from '../auth/types'

// Az API a Vite proxyn keresztül a 8080-as backendre megy (lásd vite.config.ts).
const BASE = '/api'

/** Hiba, ami a backend stabil hibakódját hordozza (a UI ezt fordítja EN/HU-ra). */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fields?: Record<string, string>

  constructor(status: number, body: ApiErrorBody) {
    super(body.message ?? body.code)
    this.status = status
    this.code = body.code
    this.fields = body.fields
  }
}

// Ha a refresh végleg elbukik, ezzel az eseménnyel jelzünk az AuthProvidernek,
// hogy állítsa "kijelentkezett" állapotra a UI-t.
export const AUTH_LOGOUT_EVENT = 'nexa:auth-logout'

function emitLogout(): void {
  window.dispatchEvent(new Event(AUTH_LOGOUT_EVENT))
}

// Egyszerre csak egy refresh fusson; a párhuzamos 401-ek ugyanarra a promise-ra várnak.
let refreshInFlight: Promise<boolean> | null = null

async function refreshTokens(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false

  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const res = await fetch(`${BASE}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        })
        if (!res.ok) return false
        const data = (await res.json()) as AuthResponse
        setTokens(data.accessToken, data.refreshToken)
        return true
      } catch {
        return false
      } finally {
        refreshInFlight = null
      }
    })()
  }
  return refreshInFlight
}

type ApiOptions = {
  method?: string
  body?: unknown
  /** true esetén nem csatolunk tokent és nem próbálunk refresh-elni (auth-végpontok). */
  auth?: boolean
}

async function rawFetch(path: string, opts: ApiOptions, withAuth: boolean): Promise<Response> {
  const headers: Record<string, string> = {}
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json'
  if (withAuth) {
    const token = getAccessToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
  }
  return fetch(`${BASE}${path}`, {
    method: opts.method ?? 'GET',
    headers,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
  })
}

/**
 * Hitelesített API-hívás JSON ki/be. 401 esetén egyszer megpróbálja frissíteni a
 * tokent és újrajátssza a kérést; ha a refresh is elbukik, kijelentkeztet.
 */
export async function apiFetch<T>(path: string, opts: ApiOptions = {}): Promise<T> {
  const useAuth = opts.auth !== false
  let res = await rawFetch(path, opts, useAuth)

  if (res.status === 401 && useAuth && getRefreshToken()) {
    const ok = await refreshTokens()
    if (ok) {
      res = await rawFetch(path, opts, true)
    } else {
      clearTokens()
      emitLogout()
    }
  }

  if (!res.ok) {
    let body: ApiErrorBody
    try {
      body = (await res.json()) as ApiErrorBody
    } catch {
      body = { code: res.status === 401 ? 'UNAUTHENTICATED' : 'UNKNOWN_ERROR' }
    }
    if (res.status === 401 && useAuth) emitLogout()
    throw new ApiError(res.status, body)
  }

  // Üres törzsű siker (204, vagy pl. 201 Created body nélkül) → nincs mit parse-olni.
  if (res.status === 204) return undefined as T
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}
