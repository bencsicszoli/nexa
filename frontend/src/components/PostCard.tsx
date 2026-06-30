import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Loader2, MoreHorizontal, Pencil, Trash2 } from 'lucide-react'
import Avatar from './Avatar'
import GroupLogo from './GroupLogo'
import Comments from './Comments'
import MediaViewer, { type ViewerItem } from './MediaViewer'
import { useAuth } from '../auth/AuthContext'
import { errorKey } from '../auth/errorKey'
import { formatRelativeTime } from '../lib/time'
import { deletePost, updatePost } from '../posts/postApi'
import type { Post, PostMedia } from '../posts/types'

const MAX_CHARS = 5000

/** A poszthoz csatolt média rácsa: képek és beágyazott videolejátszó. */
function MediaGrid({ media }: { media: PostMedia[] }) {
  const [viewerIndex, setViewerIndex] = useState<number | null>(null)
  if (media.length === 0) return null

  const cols = media.length === 1 ? 'grid-cols-1' : 'grid-cols-2'
  const images: ViewerItem[] = media
    .filter((m) => m.type !== 'VIDEO')
    .map((m) => ({ url: m.url, type: m.type }))

  // A kattintott kép indexe az images tömbben (videókat kihagyva).
  function imageIndex(mediaIndex: number): number {
    return media.slice(0, mediaIndex).filter((m) => m.type !== 'VIDEO').length
  }

  return (
    <>
      <div className={`mt-3 grid gap-2 ${cols}`}>
        {media.map((m, i) =>
          m.type === 'VIDEO' ? (
            <video
              key={i}
              src={m.url}
              controls
              preload="metadata"
              className="max-h-[28rem] w-full rounded-xl border border-slate-200 bg-black"
            />
          ) : (
            <button
              key={i}
              type="button"
              onClick={() => setViewerIndex(imageIndex(i))}
              className="block overflow-hidden rounded-xl border border-slate-200 focus:outline-none"
            >
              <img
                src={m.url}
                alt=""
                loading="lazy"
                className="max-h-[28rem] w-full object-cover"
              />
            </button>
          ),
        )}
      </div>
      {viewerIndex !== null && images.length > 0 && (
        <MediaViewer items={images} startIndex={viewerIndex} onClose={() => setViewerIndex(null)} />
      )}
    </>
  )
}

type Props = {
  post: Post
  /** Ha igaz, megjelenik a szerkesztés/törlés menü (a saját posztoknál). */
  editable?: boolean
  /**
   * Ha meg van adva (és a poszt nem szerkeszthető saját jogon), megjelenik egy
   * moderációs „Törlés" művelet, ami ezt hívja a saját törlés helyett — csoport-admin
   * idegen csoport-posztjának eltávolításához (#9 kiegészítés).
   */
  onModerateDelete?: (id: string) => Promise<void>
  /** A sikeresen szerkesztett bejegyzéssel hívódik meg. */
  onUpdated?: (post: Post) => void
  /** A sikeresen törölt bejegyzés azonosítójával hívódik meg. */
  onDeleted?: (id: string) => void
  /** Hozzászólhat-e a felhasználó ehhez a bejegyzéshez (csoportposztnál csak tag). Alap: igen. */
  canComment?: boolean
  /** Csoport-bejegyzés-e (a komment-moderáció ekkor KIZÁRÓLAG a csoport adminé, nem a posztolóé). */
  isGroupPost?: boolean
  /** Csoport-admin-e a néző — a csoport-poszt hozzászólásainak moderálásához. */
  isGroupAdmin?: boolean
  /**
   * Ha igaz és a bejegyzés csoporthoz tartozik, a fejléc jobb felső sarkában megjelenik a
   * forráscsoport logója (a csoportra mutató linkkel) — a hírfolyamban használjuk, ahol vegyesen
   * jönnek a profil- és csoportposztok. A csoportoldalon felesleges (ott minden poszt a csoporté).
   */
  showGroupBadge?: boolean
}

export default function PostCard({
  post,
  editable,
  onModerateDelete,
  onUpdated,
  onDeleted,
  canComment = true,
  isGroupPost = false,
  isGroupAdmin = false,
  showGroupBadge = false,
}: Props) {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const [editing, setEditing] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [draft, setDraft] = useState(post.content)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const menuRef = useRef<HTMLDivElement>(null)

  // Akkor van „…" menü, ha a saját posztját szerkesztheti/törölheti, vagy moderátorként törölheti.
  const showMenu = editable || !!onModerateDelete

  // Menü bezárása kívülre kattintáskor.
  useEffect(() => {
    if (!menuOpen) return
    function onClick(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [menuOpen])

  const trimmed = draft.trim()
  // Szöveg törölhető, ha a poszton van média; teljesen üres poszt nem maradhat.
  const canSave = (trimmed.length > 0 || post.media.length > 0) && !busy

  function startEdit() {
    setDraft(post.content)
    setError(null)
    setEditing(true)
    setMenuOpen(false)
  }

  function cancelEdit() {
    setEditing(false)
    setError(null)
  }

  async function onSubmitEdit(e: FormEvent) {
    e.preventDefault()
    if (!canSave) return
    setBusy(true)
    setError(null)
    try {
      const updated = await updatePost(post.id, trimmed)
      setEditing(false)
      onUpdated?.(updated)
    } catch (err) {
      setError(errorKey(err))
    } finally {
      setBusy(false)
    }
  }

  async function onConfirmDelete() {
    setBusy(true)
    setError(null)
    try {
      // Saját jogon a poszt-végpont; moderátorként a hívó által adott törlés.
      if (editable) await deletePost(post.id)
      else await onModerateDelete!(post.id)
      onDeleted?.(post.id)
    } catch (err) {
      setError(errorKey(err))
      setBusy(false)
      setConfirmingDelete(false)
    }
    // Siker esetén a kártyát a szülő eltávolítja — nincs további állapotfrissítés.
  }

  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-4">
      <header className="flex items-center gap-3">
        <Link to={`/users/${post.authorId}`} className="shrink-0">
          <Avatar name={post.authorName} src={post.authorAvatarUrl} size="md" />
        </Link>
        <div className="flex min-w-0 flex-col">
          <Link
            to={`/users/${post.authorId}`}
            className="w-fit truncate text-sm font-semibold text-slate-900 transition-colors hover:text-brand hover:underline"
          >
            {post.authorName}
          </Link>
          <time
            dateTime={post.createdAt}
            title={new Date(post.createdAt).toLocaleString(i18n.language)}
            className="text-xs text-slate-400"
          >
            {formatRelativeTime(post.createdAt, i18n.language)}
          </time>
        </div>

        <div className="ml-auto flex items-center gap-2">
          {showGroupBadge && post.group && (
            <Link
              to={`/groups/${post.group.id}`}
              title={post.group.name}
              aria-label={t('posts.inGroup', { name: post.group.name })}
              className="transition-opacity hover:opacity-80"
            >
              <GroupLogo name={post.group.name} logoUrl={post.group.logoUrl} size="sm" />
            </Link>
          )}

          {showMenu && !editing && (
            <div className="relative" ref={menuRef}>
              <button
                type="button"
                onClick={() => setMenuOpen((v) => !v)}
                aria-label={t('posts.menu')}
                aria-haspopup="menu"
                aria-expanded={menuOpen}
                className="flex h-8 w-8 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-slate-100"
              >
                <MoreHorizontal className="h-5 w-5" />
              </button>
              {menuOpen && (
                <div
                  role="menu"
                  className="absolute right-0 top-9 z-10 w-40 overflow-hidden rounded-xl border border-slate-200 bg-white py-1 shadow-lg"
                >
                  {editable && (
                    <button
                      type="button"
                      role="menuitem"
                      onClick={startEdit}
                      className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-50"
                    >
                      <Pencil className="h-4 w-4" />
                      {t('posts.edit')}
                    </button>
                  )}
                  <button
                    type="button"
                    role="menuitem"
                    onClick={() => {
                      setConfirmingDelete(true)
                      setMenuOpen(false)
                    }}
                    className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-rose-600 hover:bg-rose-50"
                  >
                    <Trash2 className="h-4 w-4" />
                    {t('posts.delete')}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </header>

      {editing ? (
        <form onSubmit={onSubmitEdit} className="mt-3">
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value.slice(0, MAX_CHARS))}
            rows={3}
            autoFocus
            className="w-full resize-none rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none transition-colors focus:border-brand focus:bg-white"
          />
          <div className="mt-2 flex items-center justify-end gap-3">
            {error && (
              <span className="mr-auto text-sm text-rose-600" role="alert">
                {t(error)}
              </span>
            )}
            <button
              type="button"
              onClick={cancelEdit}
              disabled={busy}
              className="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60"
            >
              {t('posts.cancel')}
            </button>
            <button
              type="submit"
              disabled={!canSave}
              className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
            >
              {busy && <Loader2 className="h-4 w-4 animate-spin" />}
              {busy ? t('posts.saving') : t('posts.save')}
            </button>
          </div>
        </form>
      ) : (
        <>
          {/* whitespace-pre-wrap: a felhasználó sortörései és szóközei megmaradnak */}
          {post.content && (
            <p className="mt-3 whitespace-pre-wrap break-words text-sm text-slate-700">
              {post.content}
            </p>
          )}
          <MediaGrid media={post.media} />
        </>
      )}

      {confirmingDelete && (
        <div className="mt-3 flex flex-col gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 sm:flex-row sm:items-center">
          <span className="text-sm text-rose-700">{t('posts.deleteConfirm')}</span>
          <div className="flex items-center gap-2 sm:ml-auto">
            {error && (
              <span className="text-sm text-rose-600" role="alert">
                {t(error)}
              </span>
            )}
            <button
              type="button"
              onClick={() => setConfirmingDelete(false)}
              disabled={busy}
              className="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-white disabled:opacity-60"
            >
              {t('posts.cancel')}
            </button>
            <button
              type="button"
              onClick={onConfirmDelete}
              disabled={busy}
              className="inline-flex items-center gap-2 rounded-lg bg-rose-600 px-4 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-rose-700 disabled:opacity-60"
            >
              {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
              {busy ? t('posts.deleting') : t('posts.delete')}
            </button>
          </div>
        </div>
      )}

      {!editing && (
        <Comments
          postId={post.id}
          canComment={canComment}
          // Csoportposztnál csak az admin moderál; profilposztnál a bejegyzés szerzője.
          canModerate={isGroupPost ? isGroupAdmin : post.authorId === user?.id}
        />
      )}
    </article>
  )
}
