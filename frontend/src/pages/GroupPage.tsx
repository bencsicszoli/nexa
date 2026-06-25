import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Check, FolderKanban, Loader2, LogOut, Users } from 'lucide-react'
import Avatar from '../components/Avatar'
import PostCard from '../components/PostCard'
import PostComposer from '../components/PostComposer'
import { useAuth } from '../auth/AuthContext'
import { errorKey } from '../auth/errorKey'
import {
  getGroup,
  getGroupMembers,
  getGroupPosts,
  joinGroup,
  leaveGroup,
} from '../groups/groupsApi'
import type { Group, GroupMember } from '../groups/types'
import type { Post } from '../posts/types'

export default function GroupPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const { groupId } = useParams<{ groupId: string }>()

  const [group, setGroup] = useState<Group | null>(null)
  const [members, setMembers] = useState<GroupMember[]>([])
  const [posts, setPosts] = useState<Post[]>([])

  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [feedback, setFeedback] = useState<{ kind: 'ok' | 'error'; key: string } | null>(null)

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
  }, [groupId])

  useEffect(() => {
    setLoading(true)
    setLoadError(null)
    load()
      .catch((err) => setLoadError(errorKey(err)))
      .finally(() => setLoading(false))
  }, [load])

  async function onToggleMembership() {
    if (!group) return
    setBusy(true)
    setFeedback(null)
    try {
      const updated = group.role ? await leaveGroup(group.id) : await joinGroup(group.id)
      setGroup(updated)
      setMembers(await getGroupMembers(group.id))
      setFeedback({ kind: 'ok', key: group.role ? 'groups.left' : 'groups.joined' })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setBusy(false)
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

  return (
    <div className="flex flex-col gap-4">
      <BackLink />

      <header className="rounded-2xl border border-slate-200 bg-white p-6">
        <div className="flex items-start gap-4">
          <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-brand/10 text-brand">
            <FolderKanban className="h-7 w-7" />
          </span>
          <div className="min-w-0 flex-1">
            <h1 className="text-lg font-semibold text-slate-900">{group.name}</h1>
            <p className="mt-0.5 flex items-center gap-1.5 text-sm text-slate-500">
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
              isMember
                ? 'border border-slate-200 text-slate-600 hover:bg-slate-100'
                : 'bg-brand text-white hover:bg-brand-dark'
            }`}
          >
            {busy ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : isMember ? (
              <LogOut className="h-4 w-4" />
            ) : (
              <Check className="h-4 w-4" />
            )}
            {isMember ? t('groups.leave') : t('groups.join')}
          </button>
        </div>

        {/* Tagok */}
        <div className="mt-5 border-t border-slate-100 pt-4">
          <h2 className="text-xs font-semibold uppercase tracking-wide text-slate-400">
            {t('groups.members')}
          </h2>
          <ul className="mt-3 flex flex-wrap gap-3">
            {members.map((m) => (
              <li key={m.id} className="flex items-center gap-2">
                <Avatar name={m.displayName} src={m.avatarUrl} size="sm" />
                <span className="text-sm text-slate-700">{m.displayName}</span>
                {m.role === 'ADMIN' && (
                  <span className="rounded-full bg-brand/10 px-1.5 text-[11px] font-semibold text-brand">
                    {t('groups.adminBadge')}
                  </span>
                )}
              </li>
            ))}
          </ul>
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
        posts.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            editable={post.authorId === user?.id}
            onUpdated={(updated) =>
              setPosts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)))
            }
            onDeleted={(id) => setPosts((prev) => prev.filter((p) => p.id !== id))}
          />
        ))
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
