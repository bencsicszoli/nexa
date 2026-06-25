import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Loader2, UserMinus } from 'lucide-react'
import Avatar from '../components/Avatar'
import { errorKey } from '../auth/errorKey'
import { getFollowers, getFollowing, unfollowUser } from '../follow/followApi'
import type { FollowUser } from '../follow/types'

type Tab = 'following' | 'followers'

export default function FollowingPage() {
  const { t, i18n } = useTranslation()

  const [tab, setTab] = useState<Tab>('following')
  const [following, setFollowing] = useState<FollowUser[]>([])
  const [followers, setFollowers] = useState<FollowUser[]>([])

  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<{ kind: 'ok' | 'error'; key: string } | null>(null)

  // A fülek számlálóihoz mindkét lista kell.
  const load = useCallback(async () => {
    const [fl, fr] = await Promise.all([getFollowing(), getFollowers()])
    setFollowing(fl)
    setFollowers(fr)
  }, [])

  useEffect(() => {
    load()
      .catch((err) => setFeedback({ kind: 'error', key: errorKey(err) }))
      .finally(() => setLoading(false))
  }, [load])

  async function onUnfollow(user: FollowUser) {
    setBusyId(user.id)
    setFeedback(null)
    try {
      await unfollowUser(user.id)
      await load()
      setFeedback({ kind: 'ok', key: 'follow.unfollowed' })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setBusyId(null)
    }
  }

  const sinceDate = (iso: string) =>
    new Date(iso).toLocaleDateString(i18n.language, { year: 'numeric', month: 'long', day: 'numeric' })

  const tabs: { id: Tab; label: string; badge?: number }[] = [
    { id: 'following', label: t('follow.tab.following'), badge: following.length || undefined },
    { id: 'followers', label: t('follow.tab.followers'), badge: followers.length || undefined },
  ]

  const list = tab === 'following' ? following : followers

  return (
    <div className="flex flex-col gap-4">
      <header className="rounded-2xl border border-slate-200 bg-white p-6">
        <h1 className="text-lg font-semibold text-slate-900">{t('follow.title')}</h1>
        <p className="mt-0.5 text-sm text-slate-500">{t('follow.subtitle')}</p>

        <div className="mt-4 flex gap-1 border-b border-slate-200">
          {tabs.map((tb) => (
            <button
              key={tb.id}
              type="button"
              onClick={() => {
                setTab(tb.id)
                setFeedback(null)
              }}
              className={`-mb-px flex items-center gap-2 border-b-2 px-3 py-2 text-sm font-medium transition-colors ${
                tab === tb.id
                  ? 'border-brand text-brand'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              {tb.label}
              {tb.badge != null && (
                <span className="rounded-full bg-brand/10 px-1.5 text-xs font-semibold text-brand">
                  {tb.badge}
                </span>
              )}
            </button>
          ))}
        </div>
      </header>

      {feedback && (
        <p
          className={`rounded-2xl border px-4 py-3 text-sm ${
            feedback.kind === 'ok'
              ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
              : 'border-rose-200 bg-rose-50 text-rose-600'
          }`}
          role={feedback.kind === 'error' ? 'alert' : 'status'}
        >
          {t(feedback.key)}
        </p>
      )}

      {loading ? (
        <div className="flex justify-center py-12 text-slate-400">
          <Loader2 className="h-5 w-5 animate-spin" />
        </div>
      ) : list.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-10 text-center text-sm text-slate-500">
          {tab === 'following' ? t('follow.followingEmpty') : t('follow.followersEmpty')}
        </p>
      ) : (
        <ul className="flex flex-col gap-2">
          {list.map((u) => (
            <li
              key={u.id}
              className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3"
            >
              <Avatar name={u.displayName} src={u.avatarUrl} size="lg" />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-slate-900">{u.displayName}</p>
                {u.bio ? (
                  <p className="truncate text-xs text-slate-500">{u.bio}</p>
                ) : (
                  <p className="truncate text-xs text-slate-400">
                    {t('follow.since', { date: sinceDate(u.since) })}
                  </p>
                )}
              </div>
              {tab === 'following' && (
                <button
                  type="button"
                  disabled={busyId === u.id}
                  onClick={() => onUnfollow(u)}
                  className="inline-flex shrink-0 items-center gap-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60"
                >
                  {busyId === u.id ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <UserMinus className="h-4 w-4" />
                  )}
                  {t('follow.unfollow')}
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
