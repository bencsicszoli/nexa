import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { NavLink } from 'react-router-dom'
import { Sparkles } from 'lucide-react'
import Avatar from '../Avatar'
import { getFriends } from '../../friends/friendsApi'
import type { Friend } from '../../friends/types'
import { getFollowing } from '../../follow/followApi'
import type { FollowUser } from '../../follow/types'
import { useSubscription } from '../../subscription/useSubscription'

// A jobb oldali sávban legfeljebb ennyi elemet mutatunk (a teljes lista a /friends, ill. /following oldalon).
const LIST_PREVIEW = 6

/** A hátralévő próbaidő napokban (felfelé kerekítve, sosem negatív). */
function trialDaysLeft(iso: string | null | undefined): number {
  if (!iso) return 0
  return Math.max(0, Math.ceil((new Date(iso).getTime() - Date.now()) / 86_400_000))
}

function ListCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4">
      <h2 className="mb-3 text-sm font-semibold text-slate-900">{title}</h2>
      <div className="flex flex-col gap-3">{children}</div>
    </section>
  )
}

export default function RightSidebar() {
  const { t } = useTranslation()

  const { subscription } = useSubscription()

  const [friends, setFriends] = useState<Friend[]>([])
  const [following, setFollowing] = useState<FollowUser[]>([])

  useEffect(() => {
    let active = true
    // Best-effort: a jobb sáv csak előnézet, hibát itt nem jelzünk (a /friends, /following oldal igen).
    getFriends()
      .then((list) => {
        if (active) setFriends(list)
      })
      .catch(() => {})
    getFollowing()
      .then((list) => {
        if (active) setFollowing(list)
      })
      .catch(() => {})
    return () => {
      active = false
    }
  }, [])

  return (
    <div className="flex flex-col gap-4">
      <ListCard title={t('right.friends')}>
        {friends.length === 0 ? (
          <p className="text-xs text-slate-400">{t('right.noFriends')}</p>
        ) : (
          <>
            {friends.slice(0, LIST_PREVIEW).map((f) => (
              <NavLink
                key={f.id}
                to={`/users/${f.id}`}
                className="flex items-center gap-3 transition-opacity hover:opacity-80"
              >
                <Avatar name={f.displayName} src={f.avatarUrl} size="sm" />
                <span className="truncate text-sm text-slate-700 hover:text-brand">{f.displayName}</span>
              </NavLink>
            ))}
            {friends.length > LIST_PREVIEW && (
              <NavLink to="/friends" className="text-xs font-medium text-brand hover:underline">
                {t('right.seeAllFriends', { count: friends.length })}
              </NavLink>
            )}
          </>
        )}
      </ListCard>

      <ListCard title={t('right.following')}>
        {following.length === 0 ? (
          <p className="text-xs text-slate-400">{t('right.noFollowing')}</p>
        ) : (
          <>
            {following.slice(0, LIST_PREVIEW).map((u) => (
              <NavLink
                key={u.id}
                to={`/users/${u.id}`}
                className="flex items-center gap-3 transition-opacity hover:opacity-80"
              >
                <Avatar name={u.displayName} src={u.avatarUrl} size="sm" />
                <span className="truncate text-sm text-slate-700 hover:text-brand">{u.displayName}</span>
              </NavLink>
            ))}
            {following.length > LIST_PREVIEW && (
              <NavLink to="/following" className="text-xs font-medium text-brand hover:underline">
                {t('right.seeAllFollowing', { count: following.length })}
              </NavLink>
            )}
          </>
        )}
      </ListCard>

      {/* Előfizetés-kártya — valós állapot a /api/subscriptions/me-ből (#14) */}
      <section className="rounded-2xl border border-brand/20 bg-brand/5 p-4">
        <div className="flex items-center gap-2 text-sm font-semibold text-brand">
          <Sparkles className="h-4 w-4" />
          {t('right.premiumTitle')}
        </div>
        <p className="mt-2 text-xs text-slate-600">
          {t('right.premiumDesc')}
          {subscription?.status === 'TRIALING' && (
            <>
              {' '}
              <span className="font-semibold text-slate-800">
                {t('right.premiumTrial', { days: trialDaysLeft(subscription.trialEndsAt) })}
              </span>
            </>
          )}
        </p>
        <NavLink
          to="/billing"
          className="mt-3 block w-full rounded-lg bg-brand py-2 text-center text-sm font-semibold text-white transition-colors hover:bg-brand-dark"
        >
          {t('right.premiumManage')}
        </NavLink>
      </section>
    </div>
  )
}
