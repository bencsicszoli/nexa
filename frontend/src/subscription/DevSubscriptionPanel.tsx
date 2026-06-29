import { useEffect, useState } from 'react'
import { FlaskConical, X } from 'lucide-react'
import { emitSubscriptionChanged } from './subscriptionApi'
import { getDevSubscription, setDevSubscription } from './devSubscriptionApi'
import type { SubscriptionStatus } from './types'

// FEJLESZTŐI/DEMO panel (#15) — élő Paddle nélkül állítja az előfizetés-állapotot, hogy a paywall
// megjelenése/eltűnése bemutatható legyen. KÉT réteggel védve, hogy éles buildbe sose kerüljön:
//  1) csak Vite dev-buildben renderel (import.meta.env.DEV),
//  2) csak ha a backend dev-végpontja él (a GET 404-re a panel elrejti magát).

type StateButton = {
  labelHu: string
  status: SubscriptionStatus
  trialDaysFromNow?: number
}

const BUTTONS: StateButton[] = [
  { labelHu: 'Aktív trial', status: 'TRIALING', trialDaysFromNow: 7 },
  { labelHu: 'Lejárt trial', status: 'TRIALING', trialDaysFromNow: -1 },
  { labelHu: 'Előfizetve', status: 'ACTIVE' },
  { labelHu: 'Lejárt fizetés', status: 'PAST_DUE' },
  { labelHu: 'Nincs előfizetés', status: 'NONE' },
]

export default function DevSubscriptionPanel() {
  const [available, setAvailable] = useState(false)
  const [open, setOpen] = useState(true)
  const [current, setCurrent] = useState<SubscriptionStatus | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let active = true
    getDevSubscription()
      .then((s) => {
        if (active) {
          setAvailable(true)
          setCurrent(s.status)
        }
      })
      .catch(() => {
        // 404 (flag off) vagy más hiba → a panel nem jelenik meg.
        if (active) setAvailable(false)
      })
    return () => {
      active = false
    }
  }, [])

  if (!import.meta.env.DEV || !available) return null

  async function apply(btn: StateButton) {
    setBusy(true)
    try {
      const s = await setDevSubscription(btn.status, btn.trialDaysFromNow)
      setCurrent(s.status)
      emitSubscriptionChanged()
    } finally {
      setBusy(false)
    }
  }

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="fixed bottom-4 left-4 z-50 flex h-10 w-10 items-center justify-center rounded-full bg-amber-500 text-white shadow-lg hover:bg-amber-600"
        title="Dev: előfizetés-állapot"
      >
        <FlaskConical className="h-5 w-5" />
      </button>
    )
  }

  return (
    <div className="fixed bottom-4 left-4 z-50 w-56 rounded-xl border border-amber-300 bg-white p-3 shadow-xl">
      <div className="mb-2 flex items-center justify-between">
        <span className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wide text-amber-600">
          <FlaskConical className="h-3.5 w-3.5" /> Dev · előfizetés
        </span>
        <button
          type="button"
          onClick={() => setOpen(false)}
          className="rounded p-1 text-slate-400 hover:bg-slate-100"
          aria-label="Bezárás"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      </div>
      <p className="mb-2 text-[11px] text-slate-500">
        Állapot: <span className="font-semibold text-slate-700">{current ?? '—'}</span>
      </p>
      <div className="flex flex-col gap-1.5">
        {BUTTONS.map((btn) => (
          <button
            key={btn.labelHu}
            type="button"
            disabled={busy}
            onClick={() => apply(btn)}
            className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-left text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
          >
            {btn.labelHu}
          </button>
        ))}
      </div>
    </div>
  )
}
