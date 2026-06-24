import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Loader2, ZoomIn, ZoomOut } from 'lucide-react'

// A kivágó négyzetes nézőablaka és az exportált kép mérete (px).
const VIEWPORT = 288
const OUTPUT = 512
const MAX_ZOOM = 3

type AvatarCropperProps = {
  /** A kivágandó eredeti fájl. */
  file: File
  onCancel: () => void
  /** A körre/négyzetre vágott eredmény (image/jpeg) — ezt töltjük fel. */
  onConfirm: (blob: Blob) => void
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max)
}

/**
 * Avatar-kivágó: a felhasználó mozgatással és nagyítással állítja be, mi essen a körbe.
 * A nem négyzetes képből így nem véletlenszerű rész marad, hanem a kiválasztott.
 * Az eredmény mindig négyzet, így a kör-maszk minden megjelenítési helyen helyesen vág.
 */
export default function AvatarCropper({ file, onCancel, onConfirm }: AvatarCropperProps) {
  const { t } = useTranslation()
  const [img, setImg] = useState<HTMLImageElement | null>(null)
  const [zoom, setZoom] = useState(1)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  const drag = useRef<{ px: number; py: number; ox: number; oy: number } | null>(null)

  // A „cover" alapnagyítás: ekkora skálán tölti ki a kép a nézőablakot.
  const coverScale = img ? Math.max(VIEWPORT / img.naturalWidth, VIEWPORT / img.naturalHeight) : 1
  const effective = coverScale * zoom
  const dW = img ? img.naturalWidth * effective : 0
  const dH = img ? img.naturalHeight * effective : 0

  // A fájl betöltése képpé; az URL-t felszabadítjuk.
  useEffect(() => {
    const url = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => setImg(image)
    image.src = url
    return () => URL.revokeObjectURL(url)
  }, [file])

  // Induláskor középre igazítjuk a képet.
  useEffect(() => {
    if (!img) return
    setOffset({ x: (VIEWPORT - dW) / 2, y: (VIEWPORT - dH) / 2 })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [img])

  // Az eltolás úgy korlátozva, hogy a kép mindig kitöltse a nézőablakot (ne legyen rés).
  function clampOffset(x: number, y: number) {
    return {
      x: clamp(x, VIEWPORT - dW, 0),
      y: clamp(y, VIEWPORT - dH, 0),
    }
  }

  function onPointerDown(e: ReactPointerEvent<HTMLDivElement>) {
    e.currentTarget.setPointerCapture(e.pointerId)
    drag.current = { px: e.clientX, py: e.clientY, ox: offset.x, oy: offset.y }
  }

  function onPointerMove(e: ReactPointerEvent<HTMLDivElement>) {
    if (!drag.current) return
    const next = clampOffset(
      drag.current.ox + (e.clientX - drag.current.px),
      drag.current.oy + (e.clientY - drag.current.py),
    )
    setOffset(next)
  }

  function onPointerUp() {
    drag.current = null
  }

  // Nagyításkor a nézőablak közepe körüli képpont marad fix.
  function onZoomChange(nextZoom: number) {
    const nextEffective = coverScale * nextZoom
    const centerImageX = (VIEWPORT / 2 - offset.x) / effective
    const centerImageY = (VIEWPORT / 2 - offset.y) / effective
    const nW = (img?.naturalWidth ?? 0) * nextEffective
    const nH = (img?.naturalHeight ?? 0) * nextEffective
    const x = clamp(VIEWPORT / 2 - centerImageX * nextEffective, VIEWPORT - nW, 0)
    const y = clamp(VIEWPORT / 2 - centerImageY * nextEffective, VIEWPORT - nH, 0)
    setZoom(nextZoom)
    setOffset({ x, y })
  }

  function apply() {
    if (!img) return
    const canvas = document.createElement('canvas')
    canvas.width = OUTPUT
    canvas.height = OUTPUT
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    // A nézőablak (0..VIEWPORT) leképezése a forráskép-koordinátákra.
    const sx = -offset.x / effective
    const sy = -offset.y / effective
    const sSize = VIEWPORT / effective
    ctx.drawImage(img, sx, sy, sSize, sSize, 0, 0, OUTPUT, OUTPUT)
    canvas.toBlob(
      (blob) => {
        if (blob) onConfirm(blob)
      },
      'image/jpeg',
      0.92,
    )
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      role="dialog"
      aria-modal="true"
      aria-label={t('profile.cropTitle')}
    >
      <div className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-xl">
        <h2 className="text-base font-semibold text-slate-900">{t('profile.cropTitle')}</h2>
        <p className="mt-0.5 text-sm text-slate-500">{t('profile.cropHint')}</p>

        <div className="mt-4 flex justify-center">
          <div
            className="relative cursor-move touch-none overflow-hidden rounded-lg bg-slate-100 select-none"
            style={{ width: VIEWPORT, height: VIEWPORT }}
            onPointerDown={onPointerDown}
            onPointerMove={onPointerMove}
            onPointerUp={onPointerUp}
            onPointerCancel={onPointerUp}
          >
            {img ? (
              <img
                src={img.src}
                alt=""
                draggable={false}
                className="pointer-events-none absolute max-w-none"
                style={{ width: dW, height: dH, left: offset.x, top: offset.y }}
              />
            ) : (
              <div className="flex h-full items-center justify-center">
                <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
              </div>
            )}
            {/* Kör-maszk: a körön kívüli rész elsötétítve; nem fogja el az egeret. */}
            <div
              className="pointer-events-none absolute inset-0 rounded-full"
              style={{ boxShadow: '0 0 0 9999px rgba(15, 23, 42, 0.45)' }}
            />
          </div>
        </div>

        <div className="mt-4 flex items-center gap-3">
          <ZoomOut className="h-4 w-4 shrink-0 text-slate-400" />
          <input
            type="range"
            min={1}
            max={MAX_ZOOM}
            step={0.01}
            value={zoom}
            onChange={(e) => onZoomChange(Number(e.target.value))}
            disabled={!img}
            className="h-1.5 w-full cursor-pointer accent-brand"
            aria-label={t('profile.zoom')}
          />
          <ZoomIn className="h-4 w-4 shrink-0 text-slate-400" />
        </div>

        <div className="mt-5 flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100"
          >
            {t('profile.cropCancel')}
          </button>
          <button
            type="button"
            onClick={apply}
            disabled={!img}
            className="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
          >
            {t('profile.cropApply')}
          </button>
        </div>
      </div>
    </div>
  )
}
