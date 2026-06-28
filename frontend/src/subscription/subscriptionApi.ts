import { apiFetch } from '../lib/api'
import type { CheckoutInfo, Subscription } from './types'

// A backend /api/subscriptions végpontjai (lásd com.nexa.subscription.SubscriptionController).

/**
 * Az előfizetés állapota megváltozott (sikeres checkout után). A Billing oldal és a
 * jobb sáv „Premium" kártyája erre frissül — ugyanaz a minta, mint a GROUPS_CHANGED_EVENT.
 * Mivel az állapotot a Paddle webhook írja (aszinkron), a checkout után többször is
 * elsülhet, hogy a webhook-késleltetést áthidaljuk.
 */
export const SUBSCRIPTION_CHANGED_EVENT = 'nexa:subscription-changed'

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
