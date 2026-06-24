import { useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertCircle, ImagePlus, Loader2, PenLine, Video, X } from 'lucide-react'
import Avatar from './Avatar'
import { useAuth } from '../auth/AuthContext'
import { errorKey } from '../auth/errorKey'
import {
  createPost,
  requestPostMediaUploadUrl,
  uploadPostMediaFile,
  type PostMediaInput,
} from '../posts/postApi'
import type { MediaType, Post } from '../posts/types'

const MAX_CHARS = 5000
const MAX_MEDIA = 10
const MAX_IMAGE_BYTES = 5 * 1024 * 1024 // 5 MB — egyezik a backend max-upload-bytes-szal
const MAX_VIDEO_BYTES = 50 * 1024 * 1024 // 50 MB — egyezik a backend max-video-bytes-szal
const ALLOWED_IMAGE = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const ALLOWED_VIDEO = ['video/mp4', 'video/webm']
const IMAGE_ACCEPT = ALLOWED_IMAGE.join(',')
const VIDEO_ACCEPT = ALLOWED_VIDEO.join(',')

/** A composer által kezelt egy-egy csatolt média lokális állapota. */
type Attachment = {
  id: string
  type: MediaType
  /** A böngészőben generált előnézeti URL (object URL). */
  previewUrl: string
  sizeBytes: number
  status: 'uploading' | 'done' | 'error'
  /** A tárolóbeli kulcs, amint a feltöltés kész. */
  key?: string
  /** Hibakód-i18n kulcs, ha a feltöltés elbukott. */
  errorKeyName?: string
}

type Props = {
  /** A sikeresen létrehozott bejegyzéssel hívódik meg (pl. a lista elejére fűzéshez). */
  onCreated: (post: Post) => void
}

let attachmentSeq = 0

/**
 * Szerkesztődoboz új bejegyzéshez: szöveg + kép/videó csatolás (#6 kártya).
 * A média presigned URL-re tölt fel (a posztot csak a kulccsal hozza létre).
 */
export default function PostComposer({ onCreated }: Props) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [content, setContent] = useState('')
  const [attachments, setAttachments] = useState<Attachment[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const imageInputRef = useRef<HTMLInputElement>(null)
  const videoInputRef = useRef<HTMLInputElement>(null)

  const trimmed = content.trim()
  const uploading = attachments.some((a) => a.status === 'uploading')
  const ready = attachments.filter((a) => a.status === 'done')
  const canSubmit =
    (trimmed.length > 0 || ready.length > 0) && !submitting && !uploading

  function updateAttachment(id: string, patch: Partial<Attachment>) {
    setAttachments((prev) => prev.map((a) => (a.id === id ? { ...a, ...patch } : a)))
  }

  async function onFilesSelected(e: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? [])
    e.target.value = '' // ugyanaz a fájl újra kiválasztható legyen
    if (files.length === 0) return
    setError(null)

    for (const file of files) {
      if (attachments.length >= MAX_MEDIA) {
        setError('composer.mediaTooMany')
        break
      }
      const isVideo = ALLOWED_VIDEO.includes(file.type)
      const isImage = ALLOWED_IMAGE.includes(file.type)
      if (!isVideo && !isImage) {
        setError('composer.mediaUnsupported')
        continue
      }
      const limit = isVideo ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES
      if (file.size > limit) {
        setError(isVideo ? 'composer.mediaTooLargeVideo' : 'composer.mediaTooLargeImage')
        continue
      }

      const id = `att-${attachmentSeq++}`
      const type: MediaType = isVideo ? 'VIDEO' : 'IMAGE'
      const attachment: Attachment = {
        id,
        type,
        previewUrl: URL.createObjectURL(file),
        sizeBytes: file.size,
        status: 'uploading',
      }
      setAttachments((prev) => [...prev, attachment])

      try {
        const target = await requestPostMediaUploadUrl(file.type)
        await uploadPostMediaFile(target.uploadUrl, file)
        updateAttachment(id, { status: 'done', key: target.key })
      } catch (err) {
        updateAttachment(id, { status: 'error', errorKeyName: errorKey(err) })
      }
    }
  }

  function removeAttachment(id: string) {
    setAttachments((prev) => {
      const found = prev.find((a) => a.id === id)
      if (found) URL.revokeObjectURL(found.previewUrl)
      return prev.filter((a) => a.id !== id)
    })
  }

  function reset() {
    attachments.forEach((a) => URL.revokeObjectURL(a.previewUrl))
    setContent('')
    setAttachments([])
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    setSubmitting(true)
    setError(null)
    try {
      const media: PostMediaInput[] = ready.map((a) => ({
        key: a.key!,
        type: a.type,
        sizeBytes: a.sizeBytes,
      }))
      const post = await createPost(trimmed, media)
      reset()
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

      {attachments.length > 0 && (
        <ul className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3">
          {attachments.map((a) => (
            <li
              key={a.id}
              className="group relative aspect-square overflow-hidden rounded-xl border border-slate-200 bg-slate-100"
            >
              {a.type === 'IMAGE' ? (
                <img src={a.previewUrl} alt="" className="h-full w-full object-cover" />
              ) : (
                <video src={a.previewUrl} className="h-full w-full object-cover" muted />
              )}

              {a.status === 'uploading' && (
                <div className="absolute inset-0 flex items-center justify-center bg-black/40">
                  <Loader2 className="h-6 w-6 animate-spin text-white" />
                </div>
              )}
              {a.status === 'error' && (
                <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 bg-rose-900/60 p-2 text-center text-xs text-white">
                  <AlertCircle className="h-5 w-5" />
                  <span>{t(a.errorKeyName ?? 'composer.uploadFailed')}</span>
                </div>
              )}

              <button
                type="button"
                onClick={() => removeAttachment(a.id)}
                aria-label={t('composer.removeMedia')}
                className="absolute right-1.5 top-1.5 flex h-7 w-7 items-center justify-center rounded-full bg-black/60 text-white opacity-0 transition-opacity hover:bg-black/80 focus:opacity-100 group-hover:opacity-100"
              >
                <X className="h-4 w-4" />
              </button>
            </li>
          ))}
        </ul>
      )}

      <input
        ref={imageInputRef}
        type="file"
        accept={IMAGE_ACCEPT}
        multiple
        hidden
        onChange={onFilesSelected}
      />
      <input
        ref={videoInputRef}
        type="file"
        accept={VIDEO_ACCEPT}
        hidden
        onChange={onFilesSelected}
      />

      <div className="mt-3 flex items-center justify-between gap-3 border-t border-slate-100 pt-3">
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => imageInputRef.current?.click()}
            disabled={attachments.length >= MAX_MEDIA}
            className="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-50"
          >
            <ImagePlus className="h-4 w-4 text-brand" />
            <span className="hidden sm:inline">{t('composer.photo')}</span>
          </button>
          <button
            type="button"
            onClick={() => videoInputRef.current?.click()}
            disabled={attachments.length >= MAX_MEDIA}
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
          {error && (
            <span className="text-sm text-rose-600" role="alert">
              {t(error)}
            </span>
          )}
          <button
            type="submit"
            disabled={!canSubmit}
            className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
          >
            {submitting || uploading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <PenLine className="h-4 w-4" />
            )}
            {uploading
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
