import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { useTranslation } from 'react-i18next'

type ImageLightboxProps = {
  src: string
  alt: string
  onClose: () => void
}

/**
 * Teljes képernyős, sötét hátterű kép-nagyító. ESC-re és a háttérre kattintva bezárul.
 * A megnyitás a hívó dolga (pl. Avatar `zoomable`); ez a komponens csak a megjelenítést adja.
 */
export default function ImageLightbox({ src, alt, onClose }: ImageLightboxProps) {
  const { t } = useTranslation()

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    // A háttér görgetésének tiltása, amíg a nagyítás nyitva van.
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
    }
  }, [onClose])

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
      role="dialog"
      aria-modal="true"
      aria-label={alt}
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
      <img
        src={src}
        alt={alt}
        onClick={(e) => e.stopPropagation()}
        className="max-h-[90vh] max-w-[90vw] rounded-lg object-contain shadow-2xl"
      />
    </div>,
    document.body,
  )
}
