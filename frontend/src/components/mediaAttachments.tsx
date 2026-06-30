import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { AlertCircle, Loader2, X } from 'lucide-react'
import { errorKey } from '../auth/errorKey'
import {
  requestPostMediaUploadUrl,
  uploadPostMediaFile,
  type PostMediaInput,
} from '../posts/postApi'
import type { MediaType } from '../posts/types'

// A poszt- és komment-csatolás közös korlátai és típuslistái (egyeznek a backend configgal).
export const MAX_MEDIA = 10
const MAX_IMAGE_BYTES = 5 * 1024 * 1024 // 5 MB — nexa.storage.max-upload-bytes
const MAX_VIDEO_BYTES = 50 * 1024 * 1024 // 50 MB — nexa.storage.max-video-bytes
const ALLOWED_IMAGE = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const ALLOWED_VIDEO = ['video/mp4', 'video/webm', 'video/x-matroska']
export const IMAGE_ACCEPT = ALLOWED_IMAGE.join(',')
// .mkv-nél több böngésző üres MIME-et ad, ezért a kiterjesztést is felsoroljuk.
export const VIDEO_ACCEPT = [...ALLOWED_VIDEO, '.mkv', '.mp4', '.webm'].join(',')

/**
 * A fájl média-típusa és feltöltendő MIME-je. A {@code file.type} a mérvadó, de ha üres
 * (jellemzően .mkv-nál), a kiterjesztésből következtetünk. {@code null}, ha nem támogatott.
 */
function detectMedia(file: File): { kind: MediaType; contentType: string } | null {
  if (ALLOWED_IMAGE.includes(file.type)) return { kind: 'IMAGE', contentType: file.type }
  if (ALLOWED_VIDEO.includes(file.type)) return { kind: 'VIDEO', contentType: file.type }
  if (/\.mkv$/i.test(file.name)) return { kind: 'VIDEO', contentType: 'video/x-matroska' }
  return null
}

/** Egy fájl típus- és méret-ellenőrzésének eredménye (közös a posztnak, kommentnek és a médiatárnak). */
export type MediaCheck =
  | { ok: true; kind: MediaType; contentType: string }
  | { ok: false; errorKey: string }

/** Validálja a fájl típusát és méretét; siker esetén a média-típust és a feltöltendő MIME-t adja. */
export function checkMediaFile(file: File): MediaCheck {
  const detected = detectMedia(file)
  if (!detected) return { ok: false, errorKey: 'composer.mediaUnsupported' }
  const isVideo = detected.kind === 'VIDEO'
  const limit = isVideo ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES
  if (file.size > limit) {
    return { ok: false, errorKey: isVideo ? 'composer.mediaTooLargeVideo' : 'composer.mediaTooLargeImage' }
  }
  return { ok: true, kind: detected.kind, contentType: detected.contentType }
}

/** Egy-egy csatolt média lokális állapota (feltöltés alatt / kész / hibás). */
export type Attachment = {
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

let attachmentSeq = 0

/**
 * Kép/videó-csatolás kezelése presigned feltöltéssel (közös a bejegyzés-szerkesztőnek és a
 * hozzászólás-mezőnek). Validál (típus, méret, darabszám), feltölt, és a kész elemekből
 * előállítja a létrehozáshoz szükséges média-hivatkozásokat.
 */
export function useMediaAttachments() {
  const [attachments, setAttachments] = useState<Attachment[]>([])
  const [error, setError] = useState<string | null>(null)

  function updateAttachment(id: string, patch: Partial<Attachment>) {
    setAttachments((prev) => prev.map((a) => (a.id === id ? { ...a, ...patch } : a)))
  }

  async function addFiles(files: File[]) {
    if (files.length === 0) return
    setError(null)
    // A darabszám-korlátot egy futó számlálóval ellenőrizzük (a state nem frissül a cikluson belül).
    let count = attachments.length

    for (const file of files) {
      if (count >= MAX_MEDIA) {
        setError('composer.mediaTooMany')
        break
      }
      const detected = checkMediaFile(file)
      if (!detected.ok) {
        setError(detected.errorKey)
        continue
      }

      count++
      const id = `att-${attachmentSeq++}`
      setAttachments((prev) => [
        ...prev,
        { id, type: detected.kind, previewUrl: URL.createObjectURL(file), sizeBytes: file.size, status: 'uploading' },
      ])
      try {
        const target = await requestPostMediaUploadUrl(detected.contentType)
        await uploadPostMediaFile(target.uploadUrl, file, detected.contentType)
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
    setAttachments((prev) => {
      prev.forEach((a) => URL.revokeObjectURL(a.previewUrl))
      return []
    })
    setError(null)
  }

  const uploading = attachments.some((a) => a.status === 'uploading')
  const ready = attachments.filter((a) => a.status === 'done')
  const mediaInputs: PostMediaInput[] = ready.map((a) => ({
    key: a.key!,
    type: a.type,
    sizeBytes: a.sizeBytes,
  }))

  return { attachments, addFiles, removeAttachment, reset, uploading, ready, mediaInputs, error, setError }
}

/** A csatolt médiák előnézeti rácsa eltávolító gombbal (feltöltés/hiba állapot kijelzéssel). */
export function AttachmentGrid({
  attachments,
  onRemove,
}: {
  attachments: Attachment[]
  onRemove: (id: string) => void
}) {
  const { t } = useTranslation()
  if (attachments.length === 0) return null
  return (
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
            onClick={() => onRemove(a.id)}
            aria-label={t('composer.removeMedia')}
            className="absolute right-1.5 top-1.5 flex h-7 w-7 items-center justify-center rounded-full bg-black/60 text-white opacity-0 transition-opacity hover:bg-black/80 focus:opacity-100 group-hover:opacity-100"
          >
            <X className="h-4 w-4" />
          </button>
        </li>
      ))}
    </ul>
  )
}
