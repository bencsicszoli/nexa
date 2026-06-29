import { apiFetch } from '../lib/api'
import type { Subscription, SubscriptionPlan, SubscriptionStatus } from './types'

// FEJLESZTŐI/DEMO API (#15) — a backend /api/dev/subscription végpontja csak akkor létezik,
// ha nexa.payment.dev-controls=true (egyébként 404). Élő Paddle nélkül állítja az állapotot,
// hogy a paywall demózható legyen. Élesben nincs (a flag false → a bean létre sem jön).

/** Az aktuális állapot lekérése; 404 → a dev-vezérlők ki vannak kapcsolva (a panel elrejti magát). */
export function getDevSubscription(): Promise<Subscription> {
  return apiFetch<Subscription>('/dev/subscription')
}

/** A megadott állapot beállítása (és a hozzá illő dátumok); opcionálisan a csomag is. */
export function setDevSubscription(
  status: SubscriptionStatus,
  trialDaysFromNow?: number,
  plan?: SubscriptionPlan,
): Promise<Subscription> {
  return apiFetch<Subscription>('/dev/subscription', {
    method: 'POST',
    body: { status, trialDaysFromNow, plan },
  })
}
