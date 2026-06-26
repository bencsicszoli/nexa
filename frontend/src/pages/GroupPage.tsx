import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  Check,
  Clock,
  Globe,
  Loader2,
  Lock,
  LogOut,
  UserX,
  Users,
  X,
} from 'lucide-react'
import Avatar from '../components/Avatar'
import GroupLogo from '../components/GroupLogo'
import PostCard from '../components/PostCard'
import PostComposer from '../components/PostComposer'
import { useAuth } from '../auth/AuthContext'
import { errorKey } from '../auth/errorKey'
import {
  approveJoinRequest,
  deleteGroupPost,
  getGroup,
  getGroupMembers,
  getGroupPosts,
  getJoinRequests,
  joinGroup,
  kickMember,
  leaveGroup,
  rejectJoinRequest,
} from '../groups/groupsApi'
import type { Group, GroupJoinRequest, GroupMember } from '../groups/types'
import type { Post } from '../posts/types'

export default function GroupPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const { groupId } = useParams<{ groupId: string }>()

  const [group, setGroup] = useState<Group | null>(null)
  const [members, setMembers] = useState<GroupMember[]>([])
  const [requests, setRequests] = useState<GroupJoinRequest[]>([])
  const [posts, setPosts] = useState<Post[]>([])

  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [actionId, setActionId] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<{ kind: 'ok' | 'error'; key: string } | null>(null)

  const isAdmin = group?.role === 'ADMIN'

  const load = useCallback(async () => {
    if (!groupId) return
    const [g, m, p] = await Promise.all([
      getGroup(groupId),
      getGroupMembers(groupId),
      getGroupPosts(groupId),
    ])
    setGroup(g)
    setMembers(m)
    setPosts(p)
    // A függő kérelmeket csak adminként kérjük le.
    setRequests(g.role === 'ADMIN' ? await getJoinRequests(groupId) : [])
  }, [groupId])

  useEffect(() => {
    setLoading(true)
    setLoadError(null)
    load()
      .catch((err) => setLoadError(errorKey(err)))
      .finally(() => setLoading(false))
  }, [load])

  // Egy könnyű frissítés a csoport-fej + tagok + kérelmek újratöltésére (poszt nélkül).
  const refreshMeta = useCallback(async () => {
    if (!groupId) return
    const g = await getGroup(groupId)
    setGroup(g)
    setMembers(await getGroupMembers(groupId))
    setRequests(g.role === 'ADMIN' ? await getJoinRequests(groupId) : [])
  }, [groupId])

  async function onToggleMembership() {
    if (!group) return
    setBusy(true)
    setFeedback(null)
    const leaving = group.role != null || group.requested
    try {
      const updated = leaving ? await leaveGroup(group.id) : await joinGroup(group.id)
      setGroup(updated)
      setMembers(await getGroupMembers(group.id))
      setRequests(updated.role === 'ADMIN' ? await getJoinRequests(group.id) : [])
      setFeedback({
        kind: 'ok',
        key: leaving
          ? group.requested
            ? 'groups.requestCancelled'
            : 'groups.left'
          : updated.requested
            ? 'groups.requestSent'
            : 'groups.joined',
      })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setBusy(false)
    }
  }

  // Admin műveletek (jóváhagyás / elutasítás / kizárás) egységes kezelése.
  async function runAdmin(id: string, action: () => Promise<void>, okKey: string) {
    setActionId(id)
    setFeedback(null)
    try {
      await action()
      await refreshMeta()
      setFeedback({ kind: 'ok', key: okKey })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setActionId(null)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-16 text-slate-400">
        <Loader2 className="h-5 w-5 animate-spin" />
      </div>
    )
  }

  if (loadError || !group) {
    return (
      <div className="flex flex-col gap-4">
        <BackLink />
        <p className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600" role="alert">
          {t(loadError ?? 'auth.error.GROUP_NOT_FOUND')}
        </p>
      </div>
    )
  }

  const isMember = group.role != null
  const VisIcon = group.visibility === 'PRIVATE' ? Lock : Globe
  const action = isMember
    ? { label: t('groups.leave'), Icon: LogOut, outline: true }
    : group.requested
      ? { label: t('groups.requested'), Icon: Clock, outline: true }
      : group.visibility === 'PRIVATE'
        ? { label: t('groups.requestJoin'), Icon: Lock, outline: false }
        : { label: t('groups.join'), Icon: Check, outline: false }

  return (
    <div className="flex flex-col gap-4">
      <BackLink />

      <header className="rounded-2xl border border-slate-200 bg-white p-6">
        <div className="flex items-start gap-4">
          <GroupLogo name={group.name} logoUrl={group.logoUrl} size="lg" className="h-14 w-14 text-xl" />
          <div className="min-w-0 flex-1">
            <h1 className="text-lg font-semibold text-slate-900">{group.name}</h1>
            <p className="mt-0.5 flex flex-wrap items-center gap-1.5 text-sm text-slate-500">
              <VisIcon className="h-4 w-4" />
              {t(group.visibility === 'PRIVATE' ? 'groups.private' : 'groups.public')}
              <span aria-hidden="true">·</span>
              <Users className="h-4 w-4" />
              {t('groups.memberCount', { count: group.memberCount })}
              {group.role === 'ADMIN' && (
                <span className="ml-1 rounded-full bg-brand/10 px-2 py-0.5 text-xs font-semibold text-brand">
                  {t('groups.adminBadge')}
                </span>
              )}
            </p>
            {group.description && (
              <p className="mt-3 whitespace-pre-wrap break-words text-sm text-slate-700">
                {group.description}
              </p>
            )}
          </div>
          <button
            type="button"
            disabled={busy}
            onClick={onToggleMembership}
            className={`inline-flex shrink-0 items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors disabled:opacity-60 ${
              action.outline
                ? 'border border-slate-200 text-slate-600 hover:bg-slate-100'
                : 'bg-brand text-white hover:bg-brand-dark'
            }`}
          >
            {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <action.Icon className="h-4 w-4" />}
            {action.label}
          </button>
        </div>

        {/* Tagok */}
        <div className="mt-5 border-t border-slate-100 pt-4">
          <h2 className="text-xs font-semibold uppercase tracking-wide text-slate-400">
            {t('groups.members')}
          </h2>
          <ul className="mt-3 flex flex-wrap gap-3">
            {members.map((m) => (
              <li key={m.id} className="flex items-center gap-2 rounded-full border border-slate-200 py-1 pl-1 pr-2">
                <Link
                  to={`/users/${m.id}`}
                  className="flex min-w-0 items-center gap-2 transition-opacity hover:opacity-80"
                >
                  <Avatar name={m.displayName} src={m.avatarUrl} size="sm" />
                  <span className="truncate text-sm text-slate-700">{m.displayName}</span>
                </Link>
                {m.role === 'ADMIN' && (
                  <span className="rounded-full bg-brand/10 px-1.5 text-[11px] font-semibold text-brand">
                    {t('groups.adminBadge')}
                  </span>
                )}
                {isAdmin && m.role !== 'ADMIN' && (
                  <button
                    type="button"
                    disabled={actionId === m.id}
                    onClick={() => runAdmin(m.id, () => kickMember(group.id, m.id), 'groups.kicked')}
                    aria-label={t('groups.kick')}
                    title={t('groups.kick')}
                    className="ml-0.5 flex h-5 w-5 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-rose-50 hover:text-rose-600 disabled:opacity-50"
                  >
                    {actionId === m.id ? <Loader2 className="h-3 w-3 animate-spin" /> : <X className="h-3.5 w-3.5" />}
                  </button>
                )}
              </li>
            ))}
          </ul>
        </div>

        {/* Admin: függő csatlakozási kérelmek */}
        {isAdmin && requests.length > 0 && (
          <div className="mt-5 border-t border-slate-100 pt-4">
            <h2 className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              {t('groups.pendingRequests', { count: requests.length })}
            </h2>
            <ul className="mt-3 flex flex-col gap-2">
              {requests.map((r) => (
                <li key={r.userId} className="flex items-center gap-3 rounded-xl border border-slate-200 px-3 py-2">
                  <Link
                    to={`/users/${r.userId}`}
                    className="flex min-w-0 flex-1 items-center gap-3 transition-opacity hover:opacity-80"
                  >
                    <Avatar name={r.displayName} src={r.avatarUrl} size="sm" />
                    <span className="min-w-0 flex-1 truncate text-sm font-medium text-slate-800">
                      {r.displayName}
                    </span>
                  </Link>
                  <button
                    type="button"
                    disabled={actionId === r.userId}
                    onClick={() => runAdmin(r.userId, () => approveJoinRequest(group.id, r.userId), 'groups.requestApproved')}
                    className="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
                  >
                    {actionId === r.userId ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />}
                    {t('groups.approve')}
                  </button>
                  <button
                    type="button"
                    disabled={actionId === r.userId}
                    onClick={() => runAdmin(r.userId, () => rejectJoinRequest(group.id, r.userId), 'groups.requestRejected')}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60"
                  >
                    <UserX className="h-4 w-4" />
                    {t('groups.reject')}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}
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

      {isMember ? (
        <PostComposer
          groupId={group.id}
          onCreated={(post) => setPosts((prev) => [post, ...prev])}
        />
      ) : (
        <p className="rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-6 text-center text-sm text-slate-500">
          {t('groups.joinToPost')}
        </p>
      )}

      {posts.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-10 text-center text-sm text-slate-500">
          {t('groups.postsEmpty')}
        </p>
      ) : (
        posts.map((post) => {
          const own = post.authorId === user?.id
          return (
            <PostCard
              key={post.id}
              post={post}
              canComment={isMember}
              isGroupPost
              isGroupAdmin={isAdmin}
              editable={own}
              // Idegen poszt admin-moderációs törlése a csoport-végponton.
              onModerateDelete={!own && isAdmin ? (id) => deleteGroupPost(group.id, id) : undefined}
              onUpdated={(updated) =>
                setPosts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)))
              }
              onDeleted={(id) => setPosts((prev) => prev.filter((p) => p.id !== id))}
            />
          )
        })
      )}
    </div>
  )
}

function BackLink() {
  const { t } = useTranslation()
  return (
    <Link
      to="/groups"
      className="inline-flex w-fit items-center gap-1.5 text-sm font-medium text-slate-500 transition-colors hover:text-brand"
    >
      <ArrowLeft className="h-4 w-4" />
      {t('groups.backToGroups')}
    </Link>
  )
}
