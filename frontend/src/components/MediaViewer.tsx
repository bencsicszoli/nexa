import { useCallback, useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { ChevronLeft, ChevronRight, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import type { MediaType } from '../posts/types'

export type ViewerItem = {
  url: string
  type: MediaType
}

type MediaViewerProps = {
  items: ViewerItem[]
  /** A kezdetben megjelenített elem indexe. */
  startIndex?: number
  onClose: () => void
}

/**
 * Teljes képernyős média-nézegető a médiatárhoz: balra/jobbra nyíllal (billentyű és chevron)
 * lapozható kép/videó galéria. ESC-re és a háttérre kattintva bezárul. Az {@link ImageLightbox}
 * mintáját követi, de tömböt + lapozást kezel, és videót is megjelenít.
 */
export default function MediaViewer({ items, startIndex = 0, onClose }: MediaViewerProps) {
  const { t } = useTranslation()
  const [index, setIndex] = useState(startIndex)

  const count = items.length
  const hasMany = count > 1

  const prev = useCallback(() => setIndex((i) => (i - 1 + count) % count), [count])
  const next = useCallback(() => setIndex((i) => (i + 1) % count), [count])

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
      else if (e.key === 'ArrowLeft') prev()
      else if (e.key === 'ArrowRight') next()
    }
    document.addEventListener('keydown', onKey)
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
    }
  }, [onClose, prev, next])

  const current = items[index]
  if (!current) return null

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
      role="dialog"
      aria-modal="true"
      aria-label={t('media.viewerLabel')}
      onClick={onClose}
    >
      <button
        type="button"
        onClick={onClose}
        aria-label={t('common.close')}
        className="absolute right-4 top-4 inline-flex h-10 w-10 items-center justify-center rounded-full bg-white/10 text-white transition-colors hover:bg-white/20"
      >
        <X className="h-6 w-6" />
      </button>

      {hasMany && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation()
            prev()
          }}
          aria-label={t('media.previous')}
          className="absolute left-4 top-1/2 inline-flex h-12 w-12 -translate-y-1/2 items-center justify-center rounded-full bg-white/10 text-white transition-colors hover:bg-white/20"
        >
          <ChevronLeft className="h-7 w-7" />
        </button>
      )}

      {current.type === 'VIDEO' ? (
        <video
          key={current.url}
          src={current.url}
          controls
          autoPlay
          onClick={(e) => e.stopPropagation()}
          className="max-h-[90vh] max-w-[90vw] rounded-lg bg-black shadow-2xl"
        />
      ) : (
        <img
          src={current.url}
          alt=""
          onClick={(e) => e.stopPropagation()}
          className="max-h-[90vh] max-w-[90vw] rounded-lg object-contain shadow-2xl"
        />
      )}

      {hasMany && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation()
            next()
          }}
          aria-label={t('media.next')}
          className="absolute right-4 top-1/2 inline-flex h-12 w-12 -translate-y-1/2 items-center justify-center rounded-full bg-white/10 text-white transition-colors hover:bg-white/20"
        >
          <ChevronRight className="h-7 w-7" />
        </button>
      )}

      {hasMany && (
        <span className="absolute bottom-4 left-1/2 -translate-x-1/2 rounded-full bg-black/50 px-3 py-1 text-sm text-white">
          {index + 1} / {count}
        </span>
      )}
    </div>,
    document.body,
  )
}
