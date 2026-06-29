import { useEffect, useState } from 'react'
import { getDevSubscription } from './devSubscriptionApi'

/**
 * Aktívak-e a fejlesztői/demo előfizetés-vezérlők (#15)? A backend dev-végpontja
 * ({@code GET /api/dev/subscription}) csak akkor él, ha {@code nexa.payment.dev-controls=true}
 * (egyébként 404). A `DevSubscriptionPanel` és a `BillingPage` demó-módja ennek alapján dönt —
 * éles buildben még a `import.meta.env.DEV` réteg is hozzájön a panelnél.
 */
export function useDevControls() {
  const [enabled, setEnabled] = useState(false)
  const [checked, setChecked] = useState(false)

  useEffect(() => {
    let active = true
    getDevSubscription()
      .then(() => {
        if (active) setEnabled(true)
      })
      .catch(() => {
        if (active) setEnabled(false)
      })
      .finally(() => {
        if (active) setChecked(true)
      })
    return () => {
      active = false
    }
  }, [])

  return { enabled, checked }
}
