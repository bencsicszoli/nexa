import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { Check, Clock, Loader2, MessageCircle, UserCheck, UserPlus, UserX } from 'lucide-react'
import Avatar from '../components/Avatar'
import PostCard from '../components/PostCard'
import { useAuth } from '../auth/AuthContext'
import { useChat } from '../chat/ChatContext'
import { errorKey } from '../auth/errorKey'
import { acceptRequest, removeFriend, removeRequest, sendFriendRequest } from '../friends/friendsApi'
import { followUser, unfollowUser } from '../follow/followApi'
import { getUserPosts } from '../posts/postApi'
import { getPublicProfile } from '../users/usersApi'
import type { Post } from '../posts/types'
import type { PublicUser } from '../users/types'

export default function UserProfilePage() {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const { id } = useParams<{ id: string }>()

  const [profile, setProfile] = useState<PublicUser | null>(null)
  const [posts, setPosts] = useState<Post[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionBusy, setActionBusy] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    let active = true
    setLoading(true)
    setLoadError(null)
    Promise.all([getPublicProfile(id), getUserPosts(id)])
      .then(([p, ps]) => {
        if (!active) return
        setProfile(p)
        setPosts(ps)
      })
      .catch((err) => active && setLoadError(errorKey(err)))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [id])

  // Egy kapcsolat-művelet lefuttatása, majd a profil frissítése a gombállapotokhoz.
  const runAction = useCallback(
    async (action: () => Promise<void>) => {
      if (!id) return
      setActionBusy(true)
      setActionError(null)
      try {
        await action()
        setProfile(await getPublicProfile(id))
      } catch (err) {
        setActionError(errorKey(err))
      } finally {
        setActionBusy(false)
      }
    },
    [id],
  )

  if (loading) {
    return (
      <div className="flex justify-center py-16 text-slate-400">
        <Loader2 className="h-5 w-5 animate-spin" />
      </div>
    )
  }

  if (loadError || !profile) {
    return (
      <p className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600" role="alert">
        {t(loadError ?? 'auth.error.USER_NOT_FOUND')}
      </p>
    )
  }

  // A saját profil a szerkeszthető /profile oldalon él.
  if (profile.self || profile.id === user?.id) {
    return <Navigate to="/profile" replace />
  }

  const memberSince = new Date(profile.createdAt).toLocaleDateString(i18n.language, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })

  return (
    <div className="flex flex-col gap-4">
      <header className="rounded-2xl border border-slate-200 bg-white p-6">
        <div className="flex items-start gap-4">
          <Avatar
            name={profile.displayName}
            src={profile.avatarUrl}
            size="lg"
            className="h-20 w-20 text-2xl"
          />
          <div className="min-w-0 flex-1">
            <h1 className="text-lg font-semibold text-slate-900">{profile.displayName}</h1>
            <p className="mt-0.5 text-xs text-slate-400">
              {t('profile.memberSince', { date: memberSince })}
            </p>
            {profile.bio && (
              <p className="mt-3 whitespace-pre-wrap break-words text-sm text-slate-700">{profile.bio}</p>
            )}
          </div>
        </div>

        {/* Kapcsolat-műveletek: ismerős (kétirányú) + követés (egyirányú), egymástól függetlenül */}
        <div className="mt-5 flex flex-wrap items-center gap-2 border-t border-slate-100 pt-4">
          <FriendAction profile={profile} busy={actionBusy} run={runAction} />
          <FollowAction profile={profile} busy={actionBusy} run={runAction} />
          <MessageAction userId={profile.id} />
          {actionError && (
            <span className="text-sm text-rose-600" role="alert">
              {t(actionError)}
            </span>
          )}
        </div>
      </header>

      <section className="flex flex-col gap-4">
        <h2 className="px-1 text-sm font-semibold text-slate-700">
          {t('userProfile.postsBy', { name: profile.displayName })}
        </h2>
        {posts.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-8 text-center text-sm text-slate-500">
            {t('userProfile.noPosts')}
          </p>
        ) : (
          posts.map((post) => <PostCard key={post.id} post={post} />)
        )}
      </section>
    </div>
  )
}

const PRIMARY =
  'inline-flex items-center gap-2 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60'
const OUTLINE =
  'inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60'

/** Az ismerősi kapcsolat gombja(i) a státusz alapján. */
function FriendAction({
  profile,
  busy,
  run,
}: {
  profile: PublicUser
  busy: boolean
  run: (action: () => Promise<void>) => Promise<void>
}) {
  const { t } = useTranslation()
  const Spinner = <Loader2 className="h-4 w-4 animate-spin" />

  switch (profile.friendStatus) {
    case 'FRIENDS':
      return (
        <button type="button" disabled={busy} onClick={() => run(() => removeFriend(profile.id))} className={OUTLINE}>
          {busy ? Spinner : <UserCheck className="h-4 w-4" />}
          {t('userProfile.friends')}
        </button>
      )
    case 'REQUEST_SENT':
      return (
        <button
          type="button"
          disabled={busy}
          onClick={() => profile.friendRequestId && run(() => removeRequest(profile.friendRequestId!))}
          className={OUTLINE}
        >
          {busy ? Spinner : <Clock className="h-4 w-4" />}
          {t('userProfile.requestSent')}
        </button>
      )
    case 'REQUEST_RECEIVED':
      return (
        <div className="flex items-center gap-2">
          <button
            type="button"
            disabled={busy}
            onClick={() => profile.friendRequestId && run(() => acceptRequest(profile.friendRequestId!))}
            className={PRIMARY}
          >
            {busy ? Spinner : <Check className="h-4 w-4" />}
            {t('userProfile.acceptRequest')}
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={() => profile.friendRequestId && run(() => removeRequest(profile.friendRequestId!))}
            className={OUTLINE}
          >
            <UserX className="h-4 w-4" />
            {t('userProfile.rejectRequest')}
          </button>
        </div>
      )
    default:
      return (
        <button type="button" disabled={busy} onClick={() => run(() => sendFriendRequest(profile.id))} className={PRIMARY}>
          {busy ? Spinner : <UserPlus className="h-4 w-4" />}
          {t('userProfile.addFriend')}
        </button>
      )
  }
}

/** „Üzenet" gomb: megnyitja (vagy létrehozza) a kétszemélyes szálat, és a csevegésre navigál. */
function MessageAction({ userId }: { userId: string }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { openDirect } = useChat()
  const [busy, setBusy] = useState(false)

  const open = async () => {
    setBusy(true)
    try {
      const conversation = await openDirect(userId)
      navigate(`/messages/${conversation.id}`)
    } catch {
      setBusy(false)
    }
  }

  return (
    <button type="button" disabled={busy} onClick={open} className={OUTLINE}>
      {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <MessageCircle className="h-4 w-4" />}
      {t('userProfile.message')}
    </button>
  )
}

/** A követés gombja (egyirányú, az ismerősségtől független). */
function FollowAction({
  profile,
  busy,
  run,
}: {
  profile: PublicUser
  busy: boolean
  run: (action: () => Promise<void>) => Promise<void>
}) {
  const { t } = useTranslation()
  return profile.following ? (
    <button type="button" disabled={busy} onClick={() => run(() => unfollowUser(profile.id))} className={OUTLINE}>
      {t('userProfile.following')}
    </button>
  ) : (
    <button type="button" disabled={busy} onClick={() => run(() => followUser(profile.id))} className={OUTLINE}>
      {t('userProfile.follow')}
    </button>
  )
}
