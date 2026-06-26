import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Camera, Check, Clock, Globe, Loader2, Lock, LogOut, Plus, Search, Users, X } from 'lucide-react'
import AvatarCropper from '../components/AvatarCropper'
import GroupLogo from '../components/GroupLogo'
import { errorKey } from '../auth/errorKey'
import {
  browseGroups,
  createGroup,
  getMyGroups,
  joinGroup,
  leaveGroup,
  requestGroupLogoUploadUrl,
  uploadGroupLogoFile,
} from '../groups/groupsApi'
import type { Group, GroupVisibility } from '../groups/types'

const ALLOWED_LOGO_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const LOGO_MAX_BYTES = 5 * 1024 * 1024

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
  const [visibility, setVisibility] = useState<GroupVisibility>('PUBLIC')
  const [submitting, setSubmitting] = useState(false)
  // Logó: a kivágott kép (feltöltés a létrehozáskor) + előnézeti URL + a kivágandó fájl.
  const [logoBlob, setLogoBlob] = useState<Blob | null>(null)
  const [logoPreview, setLogoPreview] = useState<string | null>(null)
  const [pendingLogo, setPendingLogo] = useState<File | null>(null)
  const logoInput = useRef<HTMLInputElement>(null)

  // A létrehozó űrlap (és a logó-előnézet) visszaállítása.
  function resetCreateForm() {
    setName('')
    setDescription('')
    setVisibility('PUBLIC')
    setLogoBlob(null)
    setLogoPreview((prev) => {
      if (prev) URL.revokeObjectURL(prev)
      return null
    })
  }

  function onPickLogo(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    e.target.value = '' // ugyanaz a fájl újra kiválasztható legyen
    if (!file) return
    setFeedback(null)
    if (!ALLOWED_LOGO_TYPES.includes(file.type)) {
      setFeedback({ kind: 'error', key: 'auth.error.UNSUPPORTED_IMAGE_TYPE' })
      return
    }
    if (file.size > LOGO_MAX_BYTES) {
      setFeedback({ kind: 'error', key: 'auth.error.PAYLOAD_TOO_LARGE' })
      return
    }
    setPendingLogo(file)
  }

  // A kivágóból kapott négyzetes kép eltárolása előnézettel (feltöltés a létrehozáskor).
  function onLogoCropped(blob: Blob) {
    setPendingLogo(null)
    setLogoBlob(blob)
    setLogoPreview((prev) => {
      if (prev) URL.revokeObjectURL(prev)
      return URL.createObjectURL(blob)
    })
  }

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
      // Logó feltöltése (ha van) a csoport létrehozása előtt — presigned URL-re.
      let logoKey: string | undefined
      if (logoBlob) {
        const target = await requestGroupLogoUploadUrl('image/jpeg')
        await uploadGroupLogoFile(target.uploadUrl, new File([logoBlob], 'logo.jpg', { type: 'image/jpeg' }))
        logoKey = target.key
      }
      await createGroup(trimmed, description.trim(), visibility, logoKey)
      resetCreateForm()
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

  // Csatlakozás / kilépés / kérelem-visszavonás a böngészésből; mindkét listát szinkronban tartjuk.
  async function onToggleMembership(group: Group) {
    setBusyId(group.id)
    setFeedback(null)
    // Tag vagy függő kérelem → kilépés/visszavonás; egyébként csatlakozás/kérelem.
    const leaving = group.role != null || group.requested
    try {
      const updated = leaving ? await leaveGroup(group.id) : await joinGroup(group.id)
      setDiscover((prev) => prev.map((g) => (g.id === group.id ? updated : g)))
      await loadMine()
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
            {/* Logó (opcionális) — kör-előnézet, a profilkép-feltöltéshez hasonlóan */}
            <div className="flex items-center gap-4">
              <GroupLogo name={name || '?'} logoUrl={logoPreview} size="lg" className="h-16 w-16 text-xl" />
              <div className="flex flex-col gap-1.5">
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => logoInput.current?.click()}
                    className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-100"
                  >
                    <Camera className="h-4 w-4" />
                    {t('groups.logoUpload')}
                  </button>
                  {logoPreview && (
                    <button
                      type="button"
                      onClick={() => {
                        setLogoBlob(null)
                        setLogoPreview((prev) => {
                          if (prev) URL.revokeObjectURL(prev)
                          return null
                        })
                      }}
                      className="inline-flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium text-slate-500 transition-colors hover:bg-slate-100"
                    >
                      <X className="h-4 w-4" />
                      {t('groups.logoRemove')}
                    </button>
                  )}
                </div>
                <span className="text-xs text-slate-400">{t('groups.logoHint')}</span>
              </div>
              <input
                ref={logoInput}
                type="file"
                accept={ALLOWED_LOGO_TYPES.join(',')}
                onChange={onPickLogo}
                className="hidden"
              />
            </div>
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
            <fieldset className="flex flex-col gap-2">
              <span className="text-sm font-medium text-slate-700">{t('groups.visibilityLabel')}</span>
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                {(['PUBLIC', 'PRIVATE'] as const).map((v) => {
                  const active = visibility === v
                  const Icon = v === 'PUBLIC' ? Globe : Lock
                  return (
                    <button
                      key={v}
                      type="button"
                      onClick={() => setVisibility(v)}
                      className={`flex items-start gap-2 rounded-lg border px-3 py-2 text-left transition-colors ${
                        active ? 'border-brand bg-brand/5 ring-1 ring-brand' : 'border-slate-200 hover:bg-slate-100'
                      }`}
                    >
                      <Icon className={`mt-0.5 h-4 w-4 shrink-0 ${active ? 'text-brand' : 'text-slate-500'}`} />
                      <span className="flex flex-col">
                        <span className="text-sm font-medium text-slate-800">
                          {t(v === 'PUBLIC' ? 'groups.public' : 'groups.private')}
                        </span>
                        <span className="text-xs text-slate-500">
                          {t(v === 'PUBLIC' ? 'groups.publicHint' : 'groups.privateHint')}
                        </span>
                      </span>
                    </button>
                  )
                })}
              </div>
            </fieldset>
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

      {pendingLogo && (
        <AvatarCropper
          file={pendingLogo}
          onCancel={() => setPendingLogo(null)}
          onConfirm={onLogoCropped}
        />
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
  const VisIcon = group.visibility === 'PRIVATE' ? Lock : Globe

  // Gombállapot: tag → kilépés; függő kérelem → visszavonás; egyébként csatlakozás
  // (publikus) / kérelem (privát).
  const variant = isMember
    ? { label: t('groups.leave'), icon: LogOut, outline: true }
    : group.requested
      ? { label: t('groups.requested'), icon: Clock, outline: true }
      : group.visibility === 'PRIVATE'
        ? { label: t('groups.requestJoin'), icon: Lock, outline: false }
        : { label: t('groups.join'), icon: Check, outline: false }
  const ActionIcon = variant.icon

  return (
    <li className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3">
      <Link
        to={`/groups/${group.id}`}
        className="flex min-w-0 flex-1 items-center gap-3 group"
      >
        <GroupLogo name={group.name} logoUrl={group.logoUrl} size="lg" className="h-11 w-11" />
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-slate-900 group-hover:text-brand">
            {group.name}
          </p>
          <p className="flex items-center gap-1 truncate text-xs text-slate-500">
            <VisIcon className="h-3.5 w-3.5" />
            {t(group.visibility === 'PRIVATE' ? 'groups.private' : 'groups.public')}
            <span aria-hidden="true">·</span>
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
          variant.outline
            ? 'border border-slate-200 text-slate-600 hover:bg-slate-100'
            : 'bg-brand text-white hover:bg-brand-dark'
        }`}
      >
        {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <ActionIcon className="h-4 w-4" />}
        {variant.label}
      </button>
    </li>
  )
}
