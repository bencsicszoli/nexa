import { apiFetch, SUBSCRIPTION_CHANGED_EVENT } from '../lib/api'
import type { CheckoutInfo, Subscription } from './types'

// A backend /api/subscriptions végpontjai (lásd com.nexa.subscription.SubscriptionController).

/**
 * Az előfizetés állapota megváltozott (sikeres checkout után, vagy 402-es paywall-jelzésnél).
 * A Billing oldal, a jobb sáv „Premium" kártyája és a paywall (#15) erre frissül. Az eseménynév
 * az api.ts-ben él (a körkörös import elkerülésére); itt re-exportáljuk a meglévő importok kedvéért.
 */
export { SUBSCRIPTION_CHANGED_EVENT }

export function emitSubscriptionChanged(): void {
  window.dispatchEvent(new Event(SUBSCRIPTION_CHANGED_EVENT))
}

/** A bejelentkezett felhasználó előfizetés-állapota (NONE, ha még nincs). */
export function getMySubscription(): Promise<Subscription> {
  return apiFetch<Subscription>('/subscriptions/me')
}

/** A Paddle overlay-checkout indításához szükséges (nem titkos) adatok. */
export function getCheckoutInfo(): Promise<CheckoutInfo> {
  return apiFetch<CheckoutInfo>('/subscriptions/checkout')
}

/** A számlázási portál megnyitható URL-je (kezelés/kártyacsere/lemondás). */
export function openPortal(): Promise<{ url: string }> {
  return apiFetch<{ url: string }>('/subscriptions/portal', { method: 'POST' })
}
