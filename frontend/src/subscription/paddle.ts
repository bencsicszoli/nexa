import { initializePaddle, type Paddle } from '@paddle/paddle-js'
import { SUBSCRIPTION_CHANGED_EVENT } from './subscriptionApi'
import type { CheckoutInfo, SubscriptionPlan } from './types'

// A Paddle.js-t egyszer inicializáljuk és gyorsítótárazzuk. A kártyaadat a Paddle
// hosztolt overlay-én megy — sosem érinti a frontend/szerver kódot.

let paddlePromise: Promise<Paddle | undefined> | null = null

async function getPaddle(info: CheckoutInfo): Promise<Paddle> {
  if (!paddlePromise) {
    paddlePromise = initializePaddle({
      environment: info.environment === 'production' ? 'production' : 'sandbox',
      token: info.clientToken,
      // A checkout lezárultát jelezzük. Mivel az állapotot a webhook írja (aszinkron),
      // többször is frissítünk, hogy a webhook-késleltetést áthidaljuk.
      eventCallback: (event) => {
        if (event?.name === 'checkout.completed') {
          const notify = () => window.dispatchEvent(new Event(SUBSCRIPTION_CHANGED_EVENT))
          notify()
          window.setTimeout(notify, 2500)
          window.setTimeout(notify, 6000)
        }
      },
    })
  }
  const paddle = await paddlePromise
  if (!paddle) {
    paddlePromise = null
    throw new Error('Paddle failed to initialize')
  }
  return paddle
}

/**
 * Megnyitja a Paddle overlay-checkoutot a kiválasztott csomaghoz. A 14 napos trial
 * magán a price-on van beállítva a Paddle oldalán; a {@code customData.userId} a
 * webhookban visszajön, így a backend a felhasználóhoz köti az előfizetést.
 */
export async function openCheckout(
  info: CheckoutInfo,
  plan: SubscriptionPlan,
  userId: string,
): Promise<void> {
  const paddle = await getPaddle(info)
  const priceId = plan === 'ANNUAL' ? info.priceAnnual : info.priceMonthly
  paddle.Checkout.open({
    items: [{ priceId, quantity: 1 }],
    customer: { email: info.customerEmail },
    customData: { userId },
    settings: { displayMode: 'overlay', theme: 'light' },
  })
}
