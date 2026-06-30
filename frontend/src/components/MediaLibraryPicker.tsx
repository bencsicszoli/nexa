import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { Check, Loader2, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { errorKey } from '../auth/errorKey'
import { getLibrary } from '../media/mediaApi'
import type { MediaItem } from '../media/types'

type MediaLibraryPickerProps = {
  /** A kiválasztott elemekből letöltött fájlokkal hívódik (a meglévő média-csatolás pipeline-hoz). */
  onPick: (files: File[]) => void
  onClose: () => void
}

const EXT_MIME: Record<string, string> = {
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp',
  gif: 'image/gif',
  mp4: 'video/mp4',
  webm: 'video/webm',
  mkv: 'video/x-matroska',
}

/** A médiatár-elem letöltése böngészőbeli {@link File}-lá, a kiterjesztésből vett MIME-mel. */
async function toFile(item: MediaItem): Promise<File> {
  const res = await fetch(item.url)
  if (!res.ok) throw new Error(`Media fetch failed: ${res.status}`)
  const blob = await res.blob()
  const name = item.url.split('/').pop() || 'media'
  const ext = name.split('.').pop()?.toLowerCase() ?? ''
  const type = EXT_MIME[ext] ?? blob.type
  return new File([blob], name, { type })
}

/**
 * Modál a médiatárból válogatáshoz: a kiválasztott kép(ek)/videó(k) beilleszthetők egy
 * bejegyzésbe vagy hozzászólásba. A kiválasztott elemeket letölti, és a hívó a meglévő
 * média-csatolás pipeline-on tölti fel a poszt/komment saját (független) másolataként.
 */
export default function MediaLibraryPicker({ onPick, onClose }: MediaLibraryPickerProps) {
  const { t } = useTranslation()
  const [items, setItems] = useState<MediaItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [inserting, setInserting] = useState(false)

  useEffect(() => {
    getLibrary()
      .then(setItems)
      .catch((err) => setError(errorKey(err)))
      .finally(() => setLoading(false))
  }, [])

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  async function onInsert() {
    const chosen = items.filter((m) => selected.has(m.id))
    if (chosen.length === 0) return
    setInserting(true)
    setError(null)
    try {
      const files = await Promise.all(chosen.map(toFile))
      onPick(files)
      onClose()
    } catch (err) {
      setError(errorKey(err))
      setInserting(false)
    }
  }

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      role="dialog"
      aria-modal="true"
      aria-label={t('media.pickerTitle')}
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="flex max-h-[85vh] w-full max-w-2xl flex-col rounded-2xl bg-white shadow-xl"
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div>
            <h2 className="text-base font-semibold text-slate-900">{t('media.pickerTitle')}</h2>
            <p className="mt-0.5 text-xs text-slate-500">{t('media.pickerHint')}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label={t('common.close')}
            className="inline-flex h-8 w-8 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-slate-100"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-4">
          {loading ? (
            <div className="flex justify-center py-12 text-slate-400">
              <Loader2 className="h-5 w-5 animate-spin" />
            </div>
          ) : items.length === 0 ? (
            <p className="py-10 text-center text-sm text-slate-500">{t('media.empty')}</p>
          ) : (
            <ul className="grid grid-cols-3 gap-2 sm:grid-cols-4">
              {items.map((item) => {
                const isSel = selected.has(item.id)
                return (
                  <li key={item.id}>
                    <button
                      type="button"
                      onClick={() => toggle(item.id)}
                      aria-pressed={isSel}
                      className={`relative block aspect-square w-full overflow-hidden rounded-xl border-2 bg-slate-100 transition-colors ${
                        isSel ? 'border-brand' : 'border-transparent hover:border-slate-300'
                      }`}
                    >
                      {item.type === 'VIDEO' ? (
                        <video src={item.url} preload="metadata" muted className="h-full w-full object-cover" />
                      ) : (
                        <img src={item.url} alt="" loading="lazy" className="h-full w-full object-cover" />
                      )}
                      {isSel && (
                        <span className="absolute right-1.5 top-1.5 flex h-6 w-6 items-center justify-center rounded-full bg-brand text-white">
                          <Check className="h-4 w-4" />
                        </span>
                      )}
                    </button>
                  </li>
                )
              })}
            </ul>
          )}
        </div>

        <div className="flex items-center justify-between gap-3 border-t border-slate-200 px-5 py-3">
          {error ? (
            <span className="text-sm text-rose-600" role="alert">
              {t(error)}
            </span>
          ) : (
            <span className="text-sm text-slate-500">{t('media.selectedCount', { count: selected.size })}</span>
          )}
          <button
            type="button"
            disabled={selected.size === 0 || inserting}
            onClick={onInsert}
            className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
          >
            {inserting && <Loader2 className="h-4 w-4 animate-spin" />}
            {t('media.insert')}
          </button>
        </div>
      </div>
    </div>,
    document.body,
  )
}
