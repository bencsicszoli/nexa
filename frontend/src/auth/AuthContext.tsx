import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import i18n, { SUPPORTED_LANGUAGES, type Language } from '../i18n'
import { AUTH_LOGOUT_EVENT } from '../lib/api'
import { getRefreshToken } from './tokenStore'
import {
  fetchMe,
  loginRequest,
  loginWith2faRequest,
  logoutRequest,
  registerRequest,
} from './authApi'
import type { LoginResult, User } from './types'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

type AuthContextValue = {
  user: User | null
  status: AuthStatus
  /** Bejelentkezés; 2FA-nál a kimenet jelzi, hogy challenge tokennel kell folytatni. */
  login: (email: string, password: string) => Promise<LoginResult>
  /** A kétlépcsős login befejezése a challenge tokennel + 2FA kóddal. */
  loginWith2fa: (challengeToken: string, code: string) => Promise<void>
  register: (email: string, displayName: string, password: string) => Promise<void>
  logout: () => Promise<void>
  /** A bejelentkezett felhasználó adatainak frissítése (pl. profilszerkesztés után). */
  updateUser: (user: User) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

/** A belépő felhasználó nyelvét tesszük az igazsággá: a UI nyelve + a mentett választás követi (#17). */
function applyUserLocale(user: User) {
  const locale = user.locale
  if (locale && (SUPPORTED_LANGUAGES as readonly string[]).includes(locale) && i18n.language !== locale) {
    void i18n.changeLanguage(locale as Language)
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [status, setStatus] = useState<AuthStatus>('loading')

  // Indításkor: ha van mentett refresh token, lekérjük a /me-t (szükség esetén
  // az api-réteg automatikusan frissíti az access tokent). Ha nincs/elbukik → kijelentkezve.
  useEffect(() => {
    let active = true
    if (!getRefreshToken()) {
      setStatus('unauthenticated')
      return
    }
    fetchMe()
      .then((me) => {
        if (!active) return
        setUser(me)
        applyUserLocale(me)
        setStatus('authenticated')
      })
      .catch(() => {
        if (!active) return
        setUser(null)
        setStatus('unauthenticated')
      })
    return () => {
      active = false
    }
  }, [])

  // Ha az api-réteg végleg kijelentkeztet (refresh bukás), igazítsuk a UI-t.
  useEffect(() => {
    const onLogout = () => {
      setUser(null)
      setStatus('unauthenticated')
    }
    window.addEventListener(AUTH_LOGOUT_EVENT, onLogout)
    return () => window.removeEventListener(AUTH_LOGOUT_EVENT, onLogout)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      login: async (email, password) => {
        const result = await loginRequest(email, password)
        if (result.kind === 'authenticated') {
          setUser(result.user)
          applyUserLocale(result.user)
          setStatus('authenticated')
        }
        return result
      },
      loginWith2fa: async (challengeToken, code) => {
        const loggedIn = await loginWith2faRequest(challengeToken, code)
        setUser(loggedIn)
        applyUserLocale(loggedIn)
        setStatus('authenticated')
      },
      register: async (email, displayName, password) => {
        const res = await registerRequest(email, displayName, password)
        setUser(res.user)
        applyUserLocale(res.user)
        setStatus('authenticated')
      },
      logout: async () => {
        await logoutRequest()
        setUser(null)
        setStatus('unauthenticated')
      },
      updateUser: (next) => setUser(next),
    }),
    [user, status],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth csak AuthProvider-en belül használható')
  return ctx
}
