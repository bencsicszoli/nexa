import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { AUTH_LOGOUT_EVENT } from '../lib/api'
import { getRefreshToken } from './tokenStore'
import {
  fetchMe,
  loginRequest,
  logoutRequest,
  registerRequest,
} from './authApi'
import type { User } from './types'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

type AuthContextValue = {
  user: User | null
  status: AuthStatus
  login: (email: string, password: string) => Promise<void>
  register: (email: string, displayName: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

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
        const res = await loginRequest(email, password)
        setUser(res.user)
        setStatus('authenticated')
      },
      register: async (email, displayName, password) => {
        const res = await registerRequest(email, displayName, password)
        setUser(res.user)
        setStatus('authenticated')
      },
      logout: async () => {
        await logoutRequest()
        setUser(null)
        setStatus('unauthenticated')
      },
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
