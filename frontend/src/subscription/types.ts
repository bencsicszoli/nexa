// A backend /api/subscriptions végpontjainak típusai (lásd com.nexa.subscription).

export type SubscriptionStatus =
  | 'NONE'
  | 'TRIALING'
  | 'ACTIVE'
  | 'PAST_DUE'
  | 'PAUSED'
  | 'CANCELED'

export type SubscriptionPlan = 'MONTHLY' | 'ANNUAL'

/** Az aktuális felhasználó előfizetés-állapota. */
export type Subscription = {
  status: SubscriptionStatus
  plan: SubscriptionPlan | null
  trialEndsAt: string | null
  renewsAt: string | null
  canceledAt: string | null
  /** Van-e hozzáférése (aktív előfizetés/folyamatban lévő próbaidő) — a backend szabálya (#15). */
  hasAccess: boolean
}

/** A Paddle overlay-checkout indításához szükséges (nem titkos) adatok. */
export type CheckoutInfo = {
  environment: string
  clientToken: string
  priceMonthly: string
  priceAnnual: string
  customerEmail: string
}
