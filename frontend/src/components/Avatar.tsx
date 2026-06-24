type AvatarProps = {
  /** Megjelenítendő név — ebből generáljuk a monogramot és a háttérszínt. */
  name: string
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

function initials(name: string): string {
  const parts = name.trim().split(/\s+/)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? parts[parts.length - 1][0] : ''
  return (first + last).toUpperCase()
}

export default function Avatar({ name, size = 'md', className = '' }: AvatarProps) {
  return (
    <span
      className={`inline-flex shrink-0 items-center justify-center rounded-full font-semibold text-white ${colorFor(
        name,
      )} ${SIZES[size]} ${className}`}
      aria-hidden="true"
    >
      {initials(name)}
    </span>
  )
}
