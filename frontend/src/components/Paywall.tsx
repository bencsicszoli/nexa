import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Lock, Sparkles } from 'lucide-react'
import type { SubscriptionStatus } from '../subscription/types'

/**
 * Teljes szélességű paywall (#15) — a hírfolyam/funkciók helyén jelenik meg, ha a hívónak
 * nincs hozzáférése (lejárt trial / nincs előfizetés / lemondott). A CTA a /billing oldalra visz,
 * ahol a próbaidő indítható / előfizetés köthető. Az állapottól függ a fejléc szövege.
 */
export default function Paywall({ status }: { status: SubscriptionStatus }) {
  const { t } = useTranslation()
  const navigate = useNavigate()

  const headlineKey =
    status === 'TRIALING'
      ? 'paywall.lapsedTrial'
      : status === 'CANCELED'
        ? 'paywall.canceled'
        : status === 'PAUSED'
          ? 'paywall.paused'
          : 'paywall.neverSubscribed'

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-brand/10 text-brand">
          <Lock className="h-7 w-7" />
        </span>
        <h1 className="mt-5 text-xl font-bold text-slate-900">{t('paywall.title')}</h1>
        <p className="mt-2 text-sm text-slate-600">{t(headlineKey)}</p>

        <button
          type="button"
          onClick={() => navigate('/billing')}
          className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-brand py-2.5 text-sm font-semibold text-white transition-colors hover:bg-brand-dark"
        >
          <Sparkles className="h-4 w-4" />
          {t('paywall.cta')}
        </button>
        <p className="mt-3 text-xs text-slate-400">{t('paywall.note')}</p>
      </div>
    </div>
  )
}
