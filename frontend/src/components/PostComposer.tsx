import { useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { ImagePlus, Loader2, PenLine, Video } from 'lucide-react'
import Avatar from './Avatar'
import { AttachmentGrid, IMAGE_ACCEPT, MAX_MEDIA, VIDEO_ACCEPT, useMediaAttachments } from './mediaAttachments'
import { useAuth } from '../auth/AuthContext'
import { errorKey } from '../auth/errorKey'
import { createPost } from '../posts/postApi'
import { createGroupPost } from '../groups/groupsApi'
import type { Post } from '../posts/types'

const MAX_CHARS = 5000

type Props = {
  /** A sikeresen létrehozott bejegyzéssel hívódik meg (pl. a lista elejére fűzéshez). */
  onCreated: (post: Post) => void
  /** Ha meg van adva, a bejegyzés ebbe a csoportba kerül (#9), nem a profilra. */
  groupId?: string
}

/**
 * Szerkesztődoboz új bejegyzéshez: szöveg + kép/videó csatolás (#6 kártya). A média presigned
 * URL-re tölt fel (a posztot csak a kulccsal hozza létre). {@code groupId} esetén a csoportba kerül.
 */
export default function PostComposer({ onCreated, groupId }: Props) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const media = useMediaAttachments()
  const imageInputRef = useRef<HTMLInputElement>(null)
  const videoInputRef = useRef<HTMLInputElement>(null)

  const trimmed = content.trim()
  const canSubmit = (trimmed.length > 0 || media.ready.length > 0) && !submitting && !media.uploading

  function onFilesSelected(e: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? [])
    e.target.value = '' // ugyanaz a fájl újra kiválasztható legyen
    void media.addFiles(files)
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    setSubmitting(true)
    setError(null)
    try {
      const post = groupId
        ? await createGroupPost(groupId, trimmed, media.mediaInputs)
        : await createPost(trimmed, media.mediaInputs)
      setContent('')
      media.reset()
      onCreated(post)
    } catch (err) {
      setError(errorKey(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={onSubmit} className="rounded-2xl border border-slate-200 bg-white p-4">
      <div className="flex items-start gap-3">
        <Avatar name={user?.displayName ?? 'Nexa'} src={user?.avatarUrl} size="md" />
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value.slice(0, MAX_CHARS))}
          rows={3}
          placeholder={t('composer.placeholder')}
          className="w-full resize-none rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none transition-colors placeholder:text-slate-400 focus:border-brand focus:bg-white"
        />
      </div>

      <AttachmentGrid attachments={media.attachments} onRemove={media.removeAttachment} />

      <input ref={imageInputRef} type="file" accept={IMAGE_ACCEPT} multiple hidden onChange={onFilesSelected} />
      <input ref={videoInputRef} type="file" accept={VIDEO_ACCEPT} hidden onChange={onFilesSelected} />

      <div className="mt-3 flex items-center justify-between gap-3 border-t border-slate-100 pt-3">
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => imageInputRef.current?.click()}
            disabled={media.attachments.length >= MAX_MEDIA}
            className="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-50"
          >
            <ImagePlus className="h-4 w-4 text-brand" />
            <span className="hidden sm:inline">{t('composer.photo')}</span>
          </button>
          <button
            type="button"
            onClick={() => videoInputRef.current?.click()}
            disabled={media.attachments.length >= MAX_MEDIA}
            className="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-50"
          >
            <Video className="h-4 w-4 text-brand" />
            <span className="hidden sm:inline">{t('composer.video')}</span>
          </button>
          <span className="ml-1 text-xs text-slate-400">
            {content.length}/{MAX_CHARS}
          </span>
        </div>

        <div className="flex items-center gap-3">
          {(error || media.error) && (
            <span className="text-sm text-rose-600" role="alert">
              {t(error ?? media.error!)}
            </span>
          )}
          <button
            type="submit"
            disabled={!canSubmit}
            className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
          >
            {submitting || media.uploading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <PenLine className="h-4 w-4" />
            )}
            {media.uploading
              ? t('composer.uploading')
              : submitting
                ? t('composer.posting')
                : t('composer.post')}
          </button>
        </div>
      </div>
    </form>
  )
}
