import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Check, Loader2, Rss, Search, UserMinus, UserPlus, X } from 'lucide-react'
import Avatar from '../components/Avatar'
import { errorKey } from '../auth/errorKey'
import { useFriendNotifications } from '../friends/FriendNotificationsContext'
import {
  acceptRequest,
  browsePeople,
  getFriends,
  getRequests,
  removeFriend,
  removeRequest,
  sendFriendRequest,
} from '../friends/friendsApi'
import type { Friend, FriendRequests, PersonSummary } from '../friends/types'
import { followUser, getFollowing, unfollowUser } from '../follow/followApi'

type Tab = 'friends' | 'requests' | 'people'

export default function FriendsPage() {
  const { t, i18n } = useTranslation()
  const { markSeen } = useFriendNotifications()

  const [tab, setTab] = useState<Tab>('friends')
  const [friends, setFriends] = useState<Friend[]>([])
  const [requests, setRequests] = useState<FriendRequests>({ incoming: [], outgoing: [] })
  const [people, setPeople] = useState<PersonSummary[]>([])
  const [query, setQuery] = useState('')
  // Akiket a felhasználó követ — az „Emberek" böngészés követés-gombjának állapotához
  // (a követés egyirányú, független az ismerősségtől; lásd /following oldal).
  const [followingIds, setFollowingIds] = useState<Set<string>>(new Set())

  const [loading, setLoading] = useState(true)
  const [peopleLoading, setPeopleLoading] = useState(false)
  // Melyik elemen fut épp egy ismerős-művelet (gomb pörög, tiltva).
  const [busyId, setBusyId] = useState<string | null>(null)
  // Külön a követés-gombhoz, mert ugyanazon a soron él, mint az ismerős-művelet.
  const [followBusyId, setFollowBusyId] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<{ kind: 'ok' | 'error'; key: string } | null>(null)

  // Ismerősök + kérések + követések betöltése (a fülek számlálóihoz / követés-gombhoz kell).
  const loadCore = useCallback(async () => {
    const [f, r, fl] = await Promise.all([getFriends(), getRequests(), getFollowing()])
    setFriends(f)
    setRequests(r)
    setFollowingIds(new Set(fl.map((u) => u.id)))
    // Az oldal megnyitásával a beérkezett kéréseket „látottá" tesszük → eltűnik a navi badge.
    markSeen(r.incoming.map((req) => req.requestId))
  }, [markSeen])

  useEffect(() => {
    loadCore()
      .catch((err) => setFeedback({ kind: 'error', key: errorKey(err) }))
      .finally(() => setLoading(false))
  }, [loadCore])

  // Az „Emberek" fül keresése — debounce-olva, hogy ne minden leütésre menjen kérés.
  useEffect(() => {
    if (tab !== 'people') return
    let active = true
    setPeopleLoading(true)
    const handle = setTimeout(() => {
      browsePeople(query)
        .then((list) => {
          if (active) setPeople(list)
        })
        .catch((err) => {
          if (active) setFeedback({ kind: 'error', key: errorKey(err) })
        })
        .finally(() => {
          if (active) setPeopleLoading(false)
        })
    }, 300)
    return () => {
      active = false
      clearTimeout(handle)
    }
  }, [tab, query])

  // Egy művelet lefuttatása egységes hibakezeléssel + az érintett listák frissítésével.
  async function run(id: string, action: () => Promise<void>, okKey: string) {
    setBusyId(id)
    setFeedback(null)
    try {
      await action()
      await loadCore()
      if (tab === 'people') setPeople(await browsePeople(query))
      setFeedback({ kind: 'ok', key: okKey })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setBusyId(null)
    }
  }

  // Követés be-/kikapcsolása az „Emberek" böngészésben (független az ismerős-művelettől).
  async function toggleFollow(person: PersonSummary) {
    const isFollowing = followingIds.has(person.id)
    setFollowBusyId(person.id)
    setFeedback(null)
    try {
      if (isFollowing) {
        await unfollowUser(person.id)
        setFollowingIds((prev) => {
          const next = new Set(prev)
          next.delete(person.id)
          return next
        })
        setFeedback({ kind: 'ok', key: 'follow.unfollowed' })
      } else {
        await followUser(person.id)
        setFollowingIds((prev) => new Set(prev).add(person.id))
        setFeedback({ kind: 'ok', key: 'follow.followed' })
      }
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setFollowBusyId(null)
    }
  }

  const memberDate = (iso: string) =>
    new Date(iso).toLocaleDateString(i18n.language, { year: 'numeric', month: 'long', day: 'numeric' })

  const tabs: { id: Tab; label: string; badge?: number }[] = [
    { id: 'friends', label: t('friends.tab.friends'), badge: friends.length || undefined },
    { id: 'requests', label: t('friends.tab.requests'), badge: requests.incoming.length || undefined },
    { id: 'people', label: t('friends.tab.people') },
  ]

  return (
    <div className="flex flex-col gap-4">
      <header className="rounded-2xl border border-slate-200 bg-white p-6">
        <h1 className="text-lg font-semibold text-slate-900">{t('friends.title')}</h1>
        <p className="mt-0.5 text-sm text-slate-500">{t('friends.subtitle')}</p>

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
      ) : tab === 'friends' ? (
        <FriendsList
          friends={friends}
          busyId={busyId}
          memberDate={memberDate}
          onRemove={(f) =>
            run(f.id, () => removeFriend(f.id), 'friends.removed')
          }
        />
      ) : tab === 'requests' ? (
        <RequestsList
          requests={requests}
          busyId={busyId}
          onAccept={(r) => run(r.requestId, () => acceptRequest(r.requestId), 'friends.accepted')}
          onDecline={(r) => run(r.requestId, () => removeRequest(r.requestId), 'friends.declined')}
          onCancel={(r) => run(r.requestId, () => removeRequest(r.requestId), 'friends.cancelled')}
        />
      ) : (
        <PeopleList
          people={people}
          query={query}
          loading={peopleLoading}
          busyId={busyId}
          followingIds={followingIds}
          followBusyId={followBusyId}
          onToggleFollow={toggleFollow}
          onQueryChange={setQuery}
          onSend={(p) => run(p.id, () => sendFriendRequest(p.id), 'friends.requestSent')}
          onAccept={(p) =>
            p.requestId && run(p.id, () => acceptRequest(p.requestId!), 'friends.accepted')
          }
          onCancel={(p) =>
            p.requestId && run(p.id, () => removeRequest(p.requestId!), 'friends.cancelled')
          }
          onRemove={(p) => run(p.id, () => removeFriend(p.id), 'friends.removed')}
        />
      )}
    </div>
  )
}

/** Egy sor egy felhasználóval (avatar + név + bio) — a jobb oldali gombokat a hívó adja.
 *  {@code userId} esetén az avatar + név a felhasználó profiljára mutató link. */
function PersonItem({
  name,
  avatarUrl,
  bio,
  meta,
  userId,
  children,
}: {
  name: string
  avatarUrl: string | null
  bio: string | null
  meta?: string
  userId?: string
  children: React.ReactNode
}) {
  const content = (
    <>
      <Avatar name={name} src={avatarUrl} size="lg" />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-semibold text-slate-900">{name}</p>
        {bio ? (
          <p className="truncate text-xs text-slate-500">{bio}</p>
        ) : meta ? (
          <p className="truncate text-xs text-slate-400">{meta}</p>
        ) : null}
      </div>
    </>
  )
  return (
    <li className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3">
      {userId ? (
        <Link
          to={`/users/${userId}`}
          className="flex min-w-0 flex-1 items-center gap-3 transition-opacity hover:opacity-80"
        >
          {content}
        </Link>
      ) : (
        <div className="flex min-w-0 flex-1 items-center gap-3">{content}</div>
      )}
      <div className="flex shrink-0 items-center gap-2">{children}</div>
    </li>
  )
}

const EMPTY =
  'rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-10 text-center text-sm text-slate-500'

function FriendsList({
  friends,
  busyId,
  memberDate,
  onRemove,
}: {
  friends: Friend[]
  busyId: string | null
  memberDate: (iso: string) => string
  onRemove: (f: Friend) => void
}) {
  const { t } = useTranslation()
  if (friends.length === 0) return <p className={EMPTY}>{t('friends.friendsEmpty')}</p>
  return (
    <ul className="flex flex-col gap-2">
      {friends.map((f) => (
        <PersonItem
          key={f.id}
          userId={f.id}
          name={f.displayName}
          avatarUrl={f.avatarUrl}
          bio={f.bio}
          meta={t('friends.since', { date: memberDate(f.friendsSince) })}
        >
          <button
            type="button"
            disabled={busyId === f.id}
            onClick={() => onRemove(f)}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60"
          >
            {busyId === f.id ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <UserMinus className="h-4 w-4" />
            )}
            {t('friends.remove')}
          </button>
        </PersonItem>
      ))}
    </ul>
  )
}

function RequestsList({
  requests,
  busyId,
  onAccept,
  onDecline,
  onCancel,
}: {
  requests: FriendRequests
  busyId: string | null
  onAccept: (r: FriendRequests['incoming'][number]) => void
  onDecline: (r: FriendRequests['incoming'][number]) => void
  onCancel: (r: FriendRequests['outgoing'][number]) => void
}) {
  const { t } = useTranslation()
  const { incoming, outgoing } = requests
  if (incoming.length === 0 && outgoing.length === 0) {
    return <p className={EMPTY}>{t('friends.requestsEmpty')}</p>
  }
  return (
    <div className="flex flex-col gap-5">
      <section className="flex flex-col gap-2">
        <h2 className="px-1 text-sm font-semibold text-slate-700">
          {t('friends.incoming')} {incoming.length > 0 && `(${incoming.length})`}
        </h2>
        {incoming.length === 0 ? (
          <p className="px-1 text-sm text-slate-400">{t('friends.incomingEmpty')}</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {incoming.map((r) => (
              <PersonItem key={r.requestId} userId={r.userId} name={r.displayName} avatarUrl={r.avatarUrl} bio={r.bio}>
                <button
                  type="button"
                  disabled={busyId === r.requestId}
                  onClick={() => onAccept(r)}
                  className="inline-flex items-center gap-2 rounded-lg bg-brand px-3 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
                >
                  {busyId === r.requestId ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Check className="h-4 w-4" />
                  )}
                  {t('friends.accept')}
                </button>
                <button
                  type="button"
                  disabled={busyId === r.requestId}
                  onClick={() => onDecline(r)}
                  className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60"
                >
                  <X className="h-4 w-4" />
                  {t('friends.decline')}
                </button>
              </PersonItem>
            ))}
          </ul>
        )}
      </section>

      {outgoing.length > 0 && (
        <section className="flex flex-col gap-2">
          <h2 className="px-1 text-sm font-semibold text-slate-700">
            {t('friends.outgoing')} ({outgoing.length})
          </h2>
          <ul className="flex flex-col gap-2">
            {outgoing.map((r) => (
              <PersonItem
                key={r.requestId}
                userId={r.userId}
                name={r.displayName}
                avatarUrl={r.avatarUrl}
                bio={r.bio}
                meta={t('friends.pending')}
              >
                <button
                  type="button"
                  disabled={busyId === r.requestId}
                  onClick={() => onCancel(r)}
                  className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60"
                >
                  {busyId === r.requestId ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <X className="h-4 w-4" />
                  )}
                  {t('friends.cancel')}
                </button>
              </PersonItem>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}

function PeopleList({
  people,
  query,
  loading,
  busyId,
  followingIds,
  followBusyId,
  onToggleFollow,
  onQueryChange,
  onSend,
  onAccept,
  onCancel,
  onRemove,
}: {
  people: PersonSummary[]
  query: string
  loading: boolean
  busyId: string | null
  followingIds: Set<string>
  followBusyId: string | null
  onToggleFollow: (p: PersonSummary) => void
  onQueryChange: (q: string) => void
  onSend: (p: PersonSummary) => void
  onAccept: (p: PersonSummary) => void
  onCancel: (p: PersonSummary) => void
  onRemove: (p: PersonSummary) => void
}) {
  const { t } = useTranslation()
  return (
    <div className="flex flex-col gap-3">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        <input
          type="search"
          value={query}
          onChange={(e) => onQueryChange(e.target.value)}
          placeholder={t('friends.peopleSearchPlaceholder')}
          className="w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-3 text-sm outline-none transition-colors focus:border-brand"
        />
      </div>

      {loading ? (
        <div className="flex justify-center py-10 text-slate-400">
          <Loader2 className="h-5 w-5 animate-spin" />
        </div>
      ) : people.length === 0 ? (
        <p className={EMPTY}>{t('friends.peopleEmpty')}</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {people.map((p) => {
            const busy = busyId === p.id
            const spin = <Loader2 className="h-4 w-4 animate-spin" />
            const isFollowing = followingIds.has(p.id)
            const followBusy = followBusyId === p.id
            return (
              <PersonItem key={p.id} userId={p.id} name={p.displayName} avatarUrl={p.avatarUrl} bio={p.bio}>
                <button
                  type="button"
                  disabled={followBusy}
                  onClick={() => onToggleFollow(p)}
                  className={`inline-flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium transition-colors disabled:opacity-60 ${
                    isFollowing
                      ? 'border border-slate-200 text-slate-600 hover:bg-slate-100'
                      : 'border border-brand/30 text-brand hover:bg-brand/10'
                  }`}
                >
                  {followBusy ? spin : <Rss className="h-4 w-4" />}
                  {isFollowing ? t('follow.following') : t('follow.follow')}
                </button>
                {p.relationship === 'NONE' && (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => onSend(p)}
                    className="inline-flex items-center gap-2 rounded-lg bg-brand px-3 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
                  >
                    {busy ? spin : <UserPlus className="h-4 w-4" />}
                    {t('friends.add')}
                  </button>
                )}
                {p.relationship === 'REQUEST_SENT' && (
                  <>
                    <span className="inline-flex items-center gap-1.5 rounded-lg bg-slate-100 px-3 py-1.5 text-sm font-medium text-slate-500">
                      <Check className="h-4 w-4" />
                      {t('friends.sent')}
                    </span>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => onCancel(p)}
                      className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60"
                      aria-label={t('friends.requested')}
                    >
                      {busy ? spin : <X className="h-4 w-4" />}
                    </button>
                  </>
                )}
                {p.relationship === 'REQUEST_RECEIVED' && (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => onAccept(p)}
                    className="inline-flex items-center gap-2 rounded-lg bg-brand px-3 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
                  >
                    {busy ? spin : <Check className="h-4 w-4" />}
                    {t('friends.accept')}
                  </button>
                )}
                {p.relationship === 'FRIENDS' && (
                  <>
                    <span className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-50 px-3 py-1.5 text-sm font-medium text-emerald-600">
                      <Check className="h-4 w-4" />
                      {t('friends.friendsLabel')}
                    </span>
                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => onRemove(p)}
                      className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60"
                      aria-label={t('friends.remove')}
                    >
                      {busy ? spin : <UserMinus className="h-4 w-4" />}
                    </button>
                  </>
                )}
              </PersonItem>
            )
          })}
        </ul>
      )}
    </div>
  )
}
