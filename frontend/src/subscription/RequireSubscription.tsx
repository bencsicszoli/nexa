import type { ReactNode } from 'react'
import { useLocation } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { useSubscription } from './useSubscription'
import Paywall from '../components/Paywall'

/**
 * Előfizetés-gating a frontenden (#15): ha a hívónak nincs hozzáférése, a védett tartalom
 * helyén a {@link Paywall} jelenik meg. A /billing mindig elérhető (hogy fizetni lehessen),
 * és betöltés közben nem villantunk paywallt. A backend ugyanezt érvényesíti (402), ez csak UX.
 */
export default function RequireSubscription({ children }: { children: ReactNode }) {
  const { subscription, loading, error } = useSubscription()
  const location = useLocation()

  // A billing oldalt sosem zárjuk le — különben a user nem tudna előfizetni.
  if (location.pathname.startsWith('/billing')) {
    return <>{children}</>
  }

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center text-slate-400">
        <Loader2 className="h-6 w-6 animate-spin" />
      </div>
    )
  }

  // Betöltési hiba esetén nem zárunk ki (fail-open): a /subscriptions/me nem gate-elt,
  // így a hiba átmeneti (hálózat/szerver) — a tényleges védelmet a backend 402-je adja.
  if (!error && subscription && !subscription.hasAccess) {
    return <Paywall status={subscription.status} />
  }

  return <>{children}</>
}
