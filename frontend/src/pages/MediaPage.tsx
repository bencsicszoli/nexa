import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { ImagePlus, Loader2, Play, Trash2 } from 'lucide-react'
import { errorKey } from '../auth/errorKey'
import { checkMediaFile, IMAGE_ACCEPT, VIDEO_ACCEPT } from '../components/mediaAttachments'
import MediaViewer from '../components/MediaViewer'
import {
  confirmLibraryMedia,
  deleteLibraryItem,
  getLibrary,
  requestLibraryUploadUrl,
  uploadLibraryFile,
} from '../media/mediaApi'
import type { MediaItem } from '../media/types'

/**
 * Médiatár: a felhasználó a bejegyzésektől függetlenül tölthet fel képet/videót, egy rácsban
 * látja őket, és egy nézegetőben nyilakkal lapozhat köztük (a megosztás posztba/kommentbe a
 * szerkesztőkben történik). Az oldalak {@code useState}+{@code useEffect} mintáját követi.
 */
export default function MediaPage() {
  const { t } = useTranslation()
  const fileInput = useRef<HTMLInputElement>(null)

  const [items, setItems] = useState<MediaItem[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [feedback, setFeedback] = useState<{ kind: 'ok' | 'error'; key: string } | null>(null)
  const [viewerIndex, setViewerIndex] = useState<number | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  useEffect(() => {
    getLibrary()
      .then(setItems)
      .catch((err) => setFeedback({ kind: 'error', key: errorKey(err) }))
      .finally(() => setLoading(false))
  }, [])

  async function onPickFiles(e: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? [])
    e.target.value = '' // ugyanaz a fájl újra kiválasztható legyen
    if (files.length === 0) return

    setFeedback(null)
    setUploading(true)
    try {
      for (const file of files) {
        const check = checkMediaFile(file)
        if (!check.ok) {
          setFeedback({ kind: 'error', key: check.errorKey })
          continue
        }
        const target = await requestLibraryUploadUrl(check.contentType)
        await uploadLibraryFile(target.uploadUrl, file, check.contentType)
        const created = await confirmLibraryMedia(target.key, check.kind, file.size)
        setItems((prev) => [created, ...prev])
      }
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setUploading(false)
    }
  }

  async function onDelete(item: MediaItem) {
    if (!window.confirm(t('media.deleteConfirm'))) return
    setBusyId(item.id)
    setFeedback(null)
    try {
      await deleteLibraryItem(item.id)
      setItems((prev) => prev.filter((m) => m.id !== item.id))
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <header className="rounded-2xl border border-slate-200 bg-white p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-lg font-semibold text-slate-900">{t('media.title')}</h1>
            <p className="mt-0.5 text-sm text-slate-500">{t('media.subtitle')}</p>
          </div>
          <button
            type="button"
            disabled={uploading}
            onClick={() => fileInput.current?.click()}
            className="inline-flex items-center gap-2 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
          >
            {uploading ? <Loader2 className="h-4 w-4 animate-spin" /> : <ImagePlus className="h-4 w-4" />}
            {uploading ? t('media.uploading') : t('media.upload')}
          </button>
          <input
            ref={fileInput}
            type="file"
            multiple
            accept={`${IMAGE_ACCEPT},${VIDEO_ACCEPT}`}
            onChange={onPickFiles}
            className="hidden"
          />
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
      ) : items.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-10 text-center text-sm text-slate-500">
          {t('media.empty')}
        </p>
      ) : (
        <ul className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
          {items.map((item, i) => (
            <li
              key={item.id}
              className="group relative aspect-square overflow-hidden rounded-xl border border-slate-200 bg-slate-100"
            >
              <button
                type="button"
                onClick={() => setViewerIndex(i)}
                aria-label={t('media.openItem')}
                className="block h-full w-full"
              >
                {item.type === 'VIDEO' ? (
                  <>
                    <video src={item.url} preload="metadata" muted className="h-full w-full object-cover" />
                    <span className="absolute inset-0 flex items-center justify-center">
                      <span className="flex h-10 w-10 items-center justify-center rounded-full bg-black/50 text-white">
                        <Play className="h-5 w-5" />
                      </span>
                    </span>
                  </>
                ) : (
                  <img src={item.url} alt="" loading="lazy" className="h-full w-full object-cover" />
                )}
              </button>
              <button
                type="button"
                disabled={busyId === item.id}
                onClick={() => onDelete(item)}
                aria-label={t('media.delete')}
                className="absolute right-1.5 top-1.5 flex h-7 w-7 items-center justify-center rounded-full bg-black/60 text-white opacity-0 transition-opacity hover:bg-black/80 focus:opacity-100 group-hover:opacity-100 disabled:opacity-60"
              >
                {busyId === item.id ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Trash2 className="h-4 w-4" />
                )}
              </button>
            </li>
          ))}
        </ul>
      )}

      {viewerIndex != null && (
        <MediaViewer
          items={items.map((m) => ({ url: m.url, type: m.type }))}
          startIndex={viewerIndex}
          onClose={() => setViewerIndex(null)}
        />
      )}
    </div>
  )
}
