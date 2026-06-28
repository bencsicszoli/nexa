import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, CreditCard, Loader2, Sparkles } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../lib/api'
import { useSubscription } from '../subscription/useSubscription'
import { getCheckoutInfo, openPortal } from '../subscription/subscriptionApi'
import { openCheckout } from '../subscription/paddle'
import type { SubscriptionPlan, SubscriptionStatus } from '../subscription/types'

/** A hátralévő próbaidő napokban (felfelé kerekítve, sosem negatív). */
function daysLeft(iso: string | null): number {
  if (!iso) return 0
  const ms = new Date(iso).getTime() - Date.now()
  return Math.max(0, Math.ceil(ms / 86_400_000))
}

export default function BillingPage() {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const { subscription, loading, error, refresh } = useSubscription()

  const [busyPlan, setBusyPlan] = useState<SubscriptionPlan | null>(null)
  const [managing, setManaging] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const status: SubscriptionStatus = subscription?.status ?? 'NONE'
  const canCheckout = status === 'NONE' || status === 'CANCELED'
  const hasBilling = status === 'TRIALING' || status === 'ACTIVE' || status === 'PAST_DUE' || status === 'PAUSED'

  const formatDate = (iso: string | null) =>
    iso ? new Date(iso).toLocaleDateString(i18n.language) : ''

  const errorMessage = (err: unknown, fallback: string) =>
    err instanceof ApiError ? t(`auth.error.${err.code}`, t('auth.error.UNKNOWN_ERROR')) : fallback

  async function handleCheckout(plan: SubscriptionPlan) {
    if (!user) return
    setBusyPlan(plan)
    setMessage(null)
    try {
      const info = await getCheckoutInfo()
      await openCheckout(info, plan, user.id)
    } catch (err) {
      setMessage(errorMessage(err, t('billing.checkoutFailed')))
    } finally {
      setBusyPlan(null)
    }
  }

  async function handleManage() {
    setManaging(true)
    setMessage(null)
    try {
      const { url } = await openPortal()
      window.open(url, '_blank', 'noopener,noreferrer')
    } catch (err) {
      setMessage(errorMessage(err, t('billing.checkoutFailed')))
    } finally {
      setManaging(false)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <header className="rounded-2xl border border-slate-200 bg-white p-6">
        <h1 className="text-lg font-semibold text-slate-900">{t('billing.title')}</h1>
        <p className="mt-0.5 text-sm text-slate-500">{t('billing.subtitle')}</p>
      </header>

      {/* Állapot-kártya */}
      <section className="rounded-2xl border border-slate-200 bg-white p-6">
        {loading ? (
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <Loader2 className="h-4 w-4 animate-spin" />
          </div>
        ) : error ? (
          <div className="flex flex-col items-start gap-2">
            <p className="text-sm text-rose-600">{t('billing.loadError')}</p>
            <button
              type="button"
              onClick={() => refresh()}
              className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              {t('billing.retry')}
            </button>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            <div className="flex items-center gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand/10 text-brand">
                <Sparkles className="h-5 w-5" />
              </span>
              <div>
                <div className="text-xs font-medium uppercase tracking-wide text-slate-400">
                  {t('billing.statusLabel')}
                </div>
                <div className="text-base font-semibold text-slate-900">
                  {t(`billing.status.${status}`)}
                </div>
              </div>
            </div>

            {/* Állapotfüggő részlet */}
            {status === 'TRIALING' && (
              <p className="text-sm text-slate-600">
                {t('billing.trialDaysLeft', { count: daysLeft(subscription?.trialEndsAt ?? null) })}
                {subscription?.trialEndsAt && (
                  <> · {t('billing.trialEnds', { date: formatDate(subscription.trialEndsAt) })}</>
                )}
              </p>
            )}
            {status === 'ACTIVE' && subscription?.renewsAt && (
              <p className="text-sm text-slate-600">
                {t('billing.renewsOn', { date: formatDate(subscription.renewsAt) })}
              </p>
            )}
            {status === 'PAST_DUE' && (
              <p className="text-sm text-amber-600">{t('billing.pastDueHint')}</p>
            )}
            {status === 'CANCELED' && subscription?.canceledAt && (
              <p className="text-sm text-slate-600">
                {t('billing.canceledOn', { date: formatDate(subscription.canceledAt) })}
              </p>
            )}
            {status === 'NONE' && <p className="text-sm text-slate-600">{t('billing.noSubHint')}</p>}

            {hasBilling && (
              <div>
                <button
                  type="button"
                  onClick={handleManage}
                  disabled={managing}
                  className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
                >
                  {managing ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <CreditCard className="h-4 w-4" />
                  )}
                  {managing ? t('billing.opening') : t('billing.manage')}
                </button>
              </div>
            )}
          </div>
        )}
      </section>

      {/* Csomagválasztás — csak amikor még nincs (vagy lemondott) előfizetés */}
      {!loading && !error && canCheckout && (
        <section className="rounded-2xl border border-slate-200 bg-white p-6">
          <h2 className="text-base font-semibold text-slate-900">{t('billing.choosePlan')}</h2>
          <p className="mt-1 text-xs text-slate-500">{t('billing.trialNote')}</p>

          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <PlanCard
              title={t('billing.planMonthly')}
              hint={t('billing.planMonthlyHint')}
              busy={busyPlan === 'MONTHLY'}
              disabled={busyPlan !== null}
              label={t('billing.startTrial')}
              onSelect={() => handleCheckout('MONTHLY')}
            />
            <PlanCard
              title={t('billing.planAnnual')}
              badge={t('billing.planAnnualBadge')}
              hint={t('billing.planAnnualHint')}
              busy={busyPlan === 'ANNUAL'}
              disabled={busyPlan !== null}
              label={t('billing.startTrial')}
              onSelect={() => handleCheckout('ANNUAL')}
            />
          </div>
        </section>
      )}

      {message && (
        <p className="rounded-lg border border-rose-200 bg-rose-50 px-4 py-2 text-sm text-rose-700">
          {message}
        </p>
      )}
    </div>
  )
}

type PlanCardProps = {
  title: string
  hint: string
  label: string
  busy: boolean
  disabled: boolean
  badge?: string
  onSelect: () => void
}

function PlanCard({ title, hint, label, busy, disabled, badge, onSelect }: PlanCardProps) {
  return (
    <div className="flex flex-col rounded-xl border border-slate-200 p-4">
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold text-slate-900">{title}</span>
        {badge && (
          <span className="rounded-full bg-brand/10 px-2 py-0.5 text-[11px] font-semibold text-brand">
            {badge}
          </span>
        )}
      </div>
      <p className="mt-1 flex items-start gap-1.5 text-xs text-slate-500">
        <Check className="mt-0.5 h-3.5 w-3.5 shrink-0 text-brand" />
        {hint}
      </p>
      <button
        type="button"
        onClick={onSelect}
        disabled={disabled}
        className="mt-4 inline-flex items-center justify-center gap-2 rounded-lg bg-brand py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
      >
        {busy && <Loader2 className="h-4 w-4 animate-spin" />}
        {label}
      </button>
    </div>
  )
}
