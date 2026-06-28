import { useCallback, useEffect, useState } from 'react'
import { AUTH_LOGOUT_EVENT } from '../lib/api'
import { SUBSCRIPTION_CHANGED_EVENT, getMySubscription } from './subscriptionApi'
import type { Subscription } from './types'

/**
 * Az aktuális előfizetés-állapot betöltése és frissen tartása. A meglévő
 * useEffect+useState+esemény mintát követi (lásd LeftNav „Csoportjaim"):
 * a SUBSCRIPTION_CHANGED_EVENT-re újratölt, kijelentkezéskor üríti.
 */
export function useSubscription() {
  const [subscription, setSubscription] = useState<Subscription | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const refresh = useCallback(() => {
    setError(false)
    return getMySubscription()
      .then((s) => setSubscription(s))
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    let active = true
    const run = () => {
      if (active) refresh()
    }
    run()
    const onLogout = () => {
      if (active) setSubscription(null)
    }
    window.addEventListener(SUBSCRIPTION_CHANGED_EVENT, run)
    window.addEventListener(AUTH_LOGOUT_EVENT, onLogout)
    return () => {
      active = false
      window.removeEventListener(SUBSCRIPTION_CHANGED_EVENT, run)
      window.removeEventListener(AUTH_LOGOUT_EVENT, onLogout)
    }
  }, [refresh])

  return { subscription, loading, error, refresh }
}
