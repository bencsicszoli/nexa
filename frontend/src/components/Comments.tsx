import { useCallback, useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { ImagePlus, Loader2, MessageCircle, Video } from 'lucide-react'
import Avatar from './Avatar'
import {
  AttachmentGrid,
  IMAGE_ACCEPT,
  MAX_MEDIA,
  VIDEO_ACCEPT,
  useMediaAttachments,
} from './mediaAttachments'
import { useAuth } from '../auth/AuthContext'
import { errorKey } from '../auth/errorKey'
import { formatRelativeTime } from '../lib/time'
import { createComment, deleteComment, getComments, updateComment } from '../comments/commentsApi'
import type { Comment } from '../comments/types'
import type { PostMediaInput } from '../posts/postApi'
import type { PostMedia } from '../posts/types'

const MAX_CHARS = 2000
// A vizuális behúzást néhány szint után nem mélyítjük tovább (mély szálaknál a hely megóvása).
const MAX_INDENT_DEPTH = 4

type Props = {
  postId: string
  /** Hozzászólhat-e a felhasználó (csoportposztnál csak tag). */
  canComment: boolean
  /** Törölheti-e a néző bármelyik hozzászólást (posztoló vagy csoport-admin). */
  canModerate: boolean
}

/**
 * Egy bejegyzés hozzászólásai fa szerkezetben (#9 kiegészítés). Alapból 1 hozzászólás látszik,
 * a többi „További N hozzászólás" linkre nyílik; a válaszok hasonlóan. Válaszolni, szerkeszteni
 * (a saját kommentet, „szerkesztve" jelzéssel) és törölni (saját / posztoló / admin) lehet.
 */
export default function Comments({ postId, canComment, canModerate }: Props) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [comments, setComments] = useState<Comment[]>([])
  const [loading, setLoading] = useState(true)
  const [expanded, setExpanded] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const reload = useCallback(async () => {
    setComments(await getComments(postId))
  }, [postId])

  useEffect(() => {
    let active = true
    getComments(postId)
      .then((list) => {
        if (active) setComments(list)
      })
      .catch((err) => {
        if (active) setError(errorKey(err))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [postId])

  const total = comments.length
  const visible = expanded ? comments : comments.slice(0, 1)

  return (
    <section className="mt-3 border-t border-slate-100 pt-3">
      {loading ? (
        <div className="flex justify-center py-2 text-slate-300">
          <Loader2 className="h-4 w-4 animate-spin" />
        </div>
      ) : (
        <>
          {total > 0 && (
            <ul className="flex flex-col gap-3">
              {visible.map((c) => (
                <CommentItem
                  key={c.id}
                  comment={c}
                  depth={0}
                  canReply={canComment}
                  canModerate={canModerate}
                  currentUserId={user?.id}
                  onChanged={reload}
                />
              ))}
            </ul>
          )}

          {!expanded && total > 1 && (
            <button
              type="button"
              onClick={() => setExpanded(true)}
              className="mt-2 text-sm font-medium text-slate-500 underline-offset-2 hover:text-brand hover:underline"
            >
              {t('comments.moreComments', { count: total - 1 })}
            </button>
          )}

          {error && (
            <p className="mt-2 text-xs text-rose-600" role="alert">
              {t(error)}
            </p>
          )}

          {canComment && (
            <div className="mt-3 flex items-start gap-2">
              <Avatar name={user?.displayName ?? 'Nexa'} src={user?.avatarUrl} size="sm" />
              <CommentInput
                placeholder={t('comments.placeholder')}
                submitLabel={t('comments.send')}
                allowMedia
                onSubmit={(text, media) => createComment(postId, text, media).then(reload)}
              />
            </div>
          )}
        </>
      )}
    </section>
  )
}

type ItemProps = {
  comment: Comment
  depth: number
  canReply: boolean
  canModerate: boolean
  currentUserId?: string
  onChanged: () => Promise<void>
}

function CommentItem({ comment, depth, canReply, canModerate, currentUserId, onChanged }: ItemProps) {
  const { t, i18n } = useTranslation()
  const [showReplies, setShowReplies] = useState(false)
  const [replying, setReplying] = useState(false)
  const [editing, setEditing] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const canEdit = comment.authorId === currentUserId
  const canDelete = canEdit || canModerate
  const replies = comment.replies ?? []
  const visibleReplies = showReplies ? replies : replies.slice(0, 1)

  async function onDelete() {
    setBusy(true)
    setError(null)
    try {
      await deleteComment(comment.id)
      await onChanged()
    } catch (err) {
      setError(errorKey(err))
      setBusy(false)
      setConfirmingDelete(false)
    }
    // Siker esetén a szülő újratölt → ez a csomópont eltűnik.
  }

  return (
    <li>
      <div className="flex items-start gap-2">
        <Avatar name={comment.authorName} src={comment.authorAvatarUrl} size="sm" />
        <div className="min-w-0 flex-1">
          {editing ? (
            <CommentInput
              initial={comment.content}
              placeholder={t('comments.placeholder')}
              submitLabel={t('comments.save')}
              autoFocus
              onCancel={() => setEditing(false)}
              onSubmit={(text) =>
                updateComment(comment.id, text).then(() => {
                  setEditing(false)
                  return onChanged()
                })
              }
            />
          ) : (
            <>
              <div className="inline-block max-w-full rounded-2xl bg-slate-100 px-3 py-2">
                <span className="text-sm font-semibold text-slate-900">{comment.authorName}</span>
                {comment.content && (
                  <p className="whitespace-pre-wrap break-words text-sm text-slate-700">
                    {comment.content}
                  </p>
                )}
              </div>
              <CommentMediaView media={comment.media} />
            </>
          )}

          {!editing && (
            <div className="mt-1 flex flex-wrap items-center gap-3 pl-3 text-xs text-slate-400">
              <time dateTime={comment.createdAt} title={new Date(comment.createdAt).toLocaleString(i18n.language)}>
                {formatRelativeTime(comment.createdAt, i18n.language)}
              </time>
              {comment.editedAt && <span>{t('comments.edited')}</span>}
              {canReply && (
                <button type="button" onClick={() => setReplying((v) => !v)} className="font-medium hover:text-brand">
                  {t('comments.reply')}
                </button>
              )}
              {canEdit && (
                <button type="button" onClick={() => setEditing(true)} className="font-medium hover:text-brand">
                  {t('comments.edit')}
                </button>
              )}
              {canDelete && !confirmingDelete && (
                <button
                  type="button"
                  onClick={() => setConfirmingDelete(true)}
                  className="font-medium hover:text-rose-600"
                >
                  {t('comments.delete')}
                </button>
              )}
              {confirmingDelete && (
                <span className="inline-flex items-center gap-2">
                  <span className="text-slate-500">{t('comments.deleteConfirm')}</span>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={onDelete}
                    className="font-semibold text-rose-600 hover:underline disabled:opacity-60"
                  >
                    {busy ? t('comments.deleting') : t('comments.delete')}
                  </button>
                  <button
                    type="button"
                    onClick={() => setConfirmingDelete(false)}
                    className="font-medium hover:text-slate-700"
                  >
                    {t('comments.cancel')}
                  </button>
                </span>
              )}
            </div>
          )}

          {error && (
            <p className="mt-1 pl-3 text-xs text-rose-600" role="alert">
              {t(error)}
            </p>
          )}

          {replying && (
            <div className="mt-2 pl-3">
              <CommentInput
                placeholder={t('comments.replyPlaceholder')}
                submitLabel={t('comments.send')}
                allowMedia
                autoFocus
                onCancel={() => setReplying(false)}
                onSubmit={(text, media) =>
                  // A válasz a komment azonosítójára érkezik (parentId).
                  createComment(comment.postId, text, media, comment.id).then(() => {
                    setReplying(false)
                    setShowReplies(true)
                    return onChanged()
                  })
                }
              />
            </div>
          )}

          {replies.length > 0 && (
            <ul
              className={`mt-2 flex flex-col gap-3 border-l border-slate-100 ${
                depth < MAX_INDENT_DEPTH ? 'pl-3' : ''
              }`}
            >
              {visibleReplies.map((r) => (
                <CommentItem
                  key={r.id}
                  comment={r}
                  depth={depth + 1}
                  canReply={canReply}
                  canModerate={canModerate}
                  currentUserId={currentUserId}
                  onChanged={onChanged}
                />
              ))}
            </ul>
          )}

          {!showReplies && replies.length > 1 && (
            <button
              type="button"
              onClick={() => setShowReplies(true)}
              className="mt-1 flex items-center gap-1 pl-3 text-xs font-medium text-slate-500 underline-offset-2 hover:text-brand hover:underline"
            >
              <MessageCircle className="h-3.5 w-3.5" />
              {t('comments.moreReplies', { count: replies.length - 1 })}
            </button>
          )}
        </div>
      </div>
    </li>
  )
}

type InputProps = {
  initial?: string
  placeholder: string
  submitLabel: string
  autoFocus?: boolean
  /** Ha igaz, kép/videó is csatolható (új hozzászólás/válasz; szerkesztésnél nem). */
  allowMedia?: boolean
  onSubmit: (text: string, media: PostMediaInput[]) => Promise<unknown>
  onCancel?: () => void
}

/** Apró szövegmező hozzászóláshoz / válaszhoz / szerkesztéshez, opcionális kép/videó csatolással. */
function CommentInput({ initial = '', placeholder, submitLabel, autoFocus, allowMedia, onSubmit, onCancel }: InputProps) {
  const { t } = useTranslation()
  const [text, setText] = useState(initial)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const media = useMediaAttachments()
  const imageInputRef = useRef<HTMLInputElement>(null)
  const videoInputRef = useRef<HTMLInputElement>(null)

  const trimmed = text.trim()
  const canSubmit = (trimmed.length > 0 || media.ready.length > 0) && !busy && !media.uploading

  function onFilesSelected(e: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? [])
    e.target.value = ''
    void media.addFiles(files)
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    setBusy(true)
    setError(null)
    try {
      await onSubmit(trimmed, media.mediaInputs)
      setText('')
      media.reset()
    } catch (err) {
      setError(errorKey(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="min-w-0 flex-1">
      <textarea
        value={text}
        onChange={(e) => setText(e.target.value.slice(0, MAX_CHARS))}
        rows={1}
        autoFocus={autoFocus}
        placeholder={placeholder}
        className="w-full resize-none rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm outline-none transition-colors placeholder:text-slate-400 focus:border-brand focus:bg-white"
      />

      {allowMedia && <AttachmentGrid attachments={media.attachments} onRemove={media.removeAttachment} />}

      {allowMedia && (
        <>
          <input ref={imageInputRef} type="file" accept={IMAGE_ACCEPT} multiple hidden onChange={onFilesSelected} />
          <input ref={videoInputRef} type="file" accept={VIDEO_ACCEPT} hidden onChange={onFilesSelected} />
        </>
      )}

      <div className="mt-1 flex flex-wrap items-center gap-2">
        {allowMedia && (
          <>
            <button
              type="button"
              onClick={() => imageInputRef.current?.click()}
              disabled={media.attachments.length >= MAX_MEDIA}
              aria-label={t('composer.photo')}
              className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 disabled:opacity-50"
            >
              <ImagePlus className="h-4 w-4 text-brand" />
            </button>
            <button
              type="button"
              onClick={() => videoInputRef.current?.click()}
              disabled={media.attachments.length >= MAX_MEDIA}
              aria-label={t('composer.video')}
              className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 disabled:opacity-50"
            >
              <Video className="h-4 w-4 text-brand" />
            </button>
          </>
        )}
        <button
          type="submit"
          disabled={!canSubmit}
          className="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-1 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
        >
          {(busy || media.uploading) && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
          {media.uploading ? t('composer.uploading') : submitLabel}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="rounded-lg px-3 py-1 text-sm font-medium text-slate-500 transition-colors hover:bg-slate-100"
          >
            {t('comments.cancel')}
          </button>
        )}
        {(error || media.error) && (
          <span className="text-xs text-rose-600" role="alert">
            {t(error ?? media.error!)}
          </span>
        )}
      </div>
    </form>
  )
}

/** A hozzászóláshoz csatolt média megjelenítése (képrács + beágyazott videó). */
function CommentMediaView({ media }: { media: PostMedia[] }) {
  if (media.length === 0) return null
  const cols = media.length === 1 ? 'grid-cols-1' : 'grid-cols-2'
  return (
    <div className={`mt-1 grid max-w-md gap-2 ${cols}`}>
      {media.map((m, i) =>
        m.type === 'VIDEO' ? (
          <video
            key={i}
            src={m.url}
            controls
            preload="metadata"
            className="max-h-72 w-full rounded-xl border border-slate-200 bg-black"
          />
        ) : (
          <a
            key={i}
            href={m.url}
            target="_blank"
            rel="noreferrer"
            className="block overflow-hidden rounded-xl border border-slate-200"
          >
            <img src={m.url} alt="" loading="lazy" className="max-h-72 w-full object-cover" />
          </a>
        ),
      )}
    </div>
  )
}
