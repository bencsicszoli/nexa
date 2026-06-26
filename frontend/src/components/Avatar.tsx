type AvatarProps = {
  /** Megjelenítendő név — ebből generáljuk a monogramot és a háttérszínt. */
  name: string
  /** Ha van feltöltött avatar-kép, ennek az URL-jét jelenítjük meg a monogram helyett. */
  src?: string | null
  /** Felülírja a névből számított monogramot (pl. csoport-logó helyőrzője — lásd GroupLogo). */
  initials?: string
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

// Fix paletta, hogy a placeholder-avatárok színe determinisztikus legyen
// (ugyanaz a név mindig ugyanazt a színt kapja). Később valódi avatar-kép váltja le (#4 kártya).
const PALETTE = [
  'bg-rose-500',
  'bg-orange-500',
  'bg-amber-500',
  'bg-emerald-500',
  'bg-teal-500',
  'bg-sky-500',
  'bg-indigo-500',
  'bg-fuchsia-500',
]

const SIZES = {
  sm: 'h-8 w-8 text-xs',
  md: 'h-10 w-10 text-sm',
  lg: 'h-12 w-12 text-base',
}

function colorFor(name: string): string {
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) | 0
  return PALETTE[Math.abs(hash) % PALETTE.length]
}

function monogramOf(name: string): string {
  const parts = name.trim().split(/\s+/)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? parts[parts.length - 1][0] : ''
  return (first + last).toUpperCase()
}

export default function Avatar({ name, src, initials, size = 'md', className = '' }: AvatarProps) {
  if (src) {
    return (
      <img
        src={src}
        alt={name}
        className={`inline-block shrink-0 rounded-full object-cover ${SIZES[size]} ${className}`}
      />
    )
  }
  return (
    <span
      className={`inline-flex shrink-0 items-center justify-center rounded-full font-semibold text-white ${colorFor(
        name,
      )} ${SIZES[size]} ${className}`}
      aria-hidden="true"
    >
      {initials ?? monogramOf(name)}
    </span>
  )
}
