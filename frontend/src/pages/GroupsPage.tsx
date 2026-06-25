import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Check, FolderKanban, Loader2, LogOut, Plus, Search, Users } from 'lucide-react'
import { errorKey } from '../auth/errorKey'
import {
  browseGroups,
  createGroup,
  getMyGroups,
  joinGroup,
  leaveGroup,
} from '../groups/groupsApi'
import type { Group } from '../groups/types'

type Tab = 'mine' | 'discover'

const NAME_MAX = 80
const DESC_MAX = 500

export default function GroupsPage() {
  const { t } = useTranslation()

  const [tab, setTab] = useState<Tab>('mine')
  const [mine, setMine] = useState<Group[]>([])
  const [discover, setDiscover] = useState<Group[]>([])
  const [query, setQuery] = useState('')

  const [loading, setLoading] = useState(true)
  const [discoverLoading, setDiscoverLoading] = useState(false)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<{ kind: 'ok' | 'error'; key: string } | null>(null)

  // Létrehozás űrlap
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const loadMine = useCallback(async () => {
    setMine(await getMyGroups())
  }, [])

  useEffect(() => {
    loadMine()
      .catch((err) => setFeedback({ kind: 'error', key: errorKey(err) }))
      .finally(() => setLoading(false))
  }, [loadMine])

  // A „Felfedezés" fül keresése — debounce-olva.
  useEffect(() => {
    if (tab !== 'discover') return
    let active = true
    setDiscoverLoading(true)
    const handle = setTimeout(() => {
      browseGroups(query)
        .then((list) => {
          if (active) setDiscover(list)
        })
        .catch((err) => {
          if (active) setFeedback({ kind: 'error', key: errorKey(err) })
        })
        .finally(() => {
          if (active) setDiscoverLoading(false)
        })
    }, 300)
    return () => {
      active = false
      clearTimeout(handle)
    }
  }, [tab, query])

  async function onCreate(e: FormEvent) {
    e.preventDefault()
    const trimmed = name.trim()
    if (trimmed.length < 2 || submitting) return
    setSubmitting(true)
    setFeedback(null)
    try {
      await createGroup(trimmed, description.trim())
      setName('')
      setDescription('')
      setCreating(false)
      await loadMine()
      setTab('mine')
      setFeedback({ kind: 'ok', key: 'groups.created' })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setSubmitting(false)
    }
  }

  // Csatlakozás / kilépés a böngészésből; mindkét listát szinkronban tartjuk.
  async function onToggleMembership(group: Group) {
    setBusyId(group.id)
    setFeedback(null)
    try {
      const updated = group.role ? await leaveGroup(group.id) : await joinGroup(group.id)
      setDiscover((prev) => prev.map((g) => (g.id === group.id ? updated : g)))
      await loadMine()
      setFeedback({ kind: 'ok', key: group.role ? 'groups.left' : 'groups.joined' })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setBusyId(null)
    }
  }

  const tabs: { id: Tab; label: string; badge?: number }[] = [
    { id: 'mine', label: t('groups.tab.mine'), badge: mine.length || undefined },
    { id: 'discover', label: t('groups.tab.discover') },
  ]

  return (
    <div className="flex flex-col gap-4">
      <header className="rounded-2xl border border-slate-200 bg-white p-6">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h1 className="text-lg font-semibold text-slate-900">{t('groups.title')}</h1>
            <p className="mt-0.5 text-sm text-slate-500">{t('groups.subtitle')}</p>
          </div>
          <button
            type="button"
            onClick={() => setCreating((v) => !v)}
            className="inline-flex shrink-0 items-center gap-2 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark"
          >
            <Plus className="h-4 w-4" />
            <span className="hidden sm:inline">{t('groups.create')}</span>
          </button>
        </div>

        {creating && (
          <form onSubmit={onCreate} className="mt-4 flex flex-col gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4">
            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium text-slate-700">{t('groups.nameLabel')}</span>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value.slice(0, NAME_MAX))}
                placeholder={t('groups.namePlaceholder')}
                minLength={2}
                required
                autoFocus
                className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none transition-colors focus:border-brand"
              />
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium text-slate-700">{t('groups.descriptionLabel')}</span>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value.slice(0, DESC_MAX))}
                rows={2}
                placeholder={t('groups.descriptionPlaceholder')}
                className="resize-none rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none transition-colors focus:border-brand"
              />
            </label>
            <div className="flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setCreating(false)}
                className="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100"
              >
                {t('groups.cancel')}
              </button>
              <button
                type="submit"
                disabled={name.trim().length < 2 || submitting}
                className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
              >
                {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
                {submitting ? t('groups.creating') : t('groups.create')}
              </button>
            </div>
          </form>
        )}

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
      ) : tab === 'mine' ? (
        mine.length === 0 ? (
          <p className={EMPTY}>{t('groups.mineEmpty')}</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {mine.map((g) => (
              <GroupRow key={g.id} group={g} busyId={busyId} onToggle={onToggleMembership} />
            ))}
          </ul>
        )
      ) : (
        <div className="flex flex-col gap-3">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t('groups.searchPlaceholder')}
              className="w-full rounded-xl border border-slate-200 bg-white py-2.5 pl-10 pr-3 text-sm outline-none transition-colors focus:border-brand"
            />
          </div>
          {discoverLoading ? (
            <div className="flex justify-center py-10 text-slate-400">
              <Loader2 className="h-5 w-5 animate-spin" />
            </div>
          ) : discover.length === 0 ? (
            <p className={EMPTY}>{t('groups.discoverEmpty')}</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {discover.map((g) => (
                <GroupRow key={g.id} group={g} busyId={busyId} onToggle={onToggleMembership} />
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}

const EMPTY =
  'rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-10 text-center text-sm text-slate-500'

/** Egy csoport sora: ikon + név/leírás (a részletekre mutató link) + csatlakozás/kilépés. */
function GroupRow({
  group,
  busyId,
  onToggle,
}: {
  group: Group
  busyId: string | null
  onToggle: (g: Group) => void
}) {
  const { t } = useTranslation()
  const busy = busyId === group.id
  const isMember = group.role != null

  return (
    <li className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3">
      <Link
        to={`/groups/${group.id}`}
        className="flex min-w-0 flex-1 items-center gap-3 group"
      >
        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand/10 text-brand">
          <FolderKanban className="h-5 w-5" />
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-slate-900 group-hover:text-brand">
            {group.name}
          </p>
          <p className="flex items-center gap-1 truncate text-xs text-slate-500">
            <Users className="h-3.5 w-3.5" />
            {t('groups.memberCount', { count: group.memberCount })}
            {group.role === 'ADMIN' && (
              <span className="ml-1 rounded-full bg-brand/10 px-1.5 text-[11px] font-semibold text-brand">
                {t('groups.adminBadge')}
              </span>
            )}
          </p>
        </div>
      </Link>
      <button
        type="button"
        disabled={busy}
        onClick={() => onToggle(group)}
        className={`inline-flex shrink-0 items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium transition-colors disabled:opacity-60 ${
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
    </li>
  )
}
