import Avatar from './Avatar'

/**
 * A csoport monogramja, ha nincs feltöltött logó:
 * - több szóból álló név → az első két szó kezdőbetűi (pl. „Túra Kör" → „TK"),
 * - egy szóból álló név → az első két betű (pl. „Kódklub" → „KÓ").
 */
export function groupInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[1][0]).toUpperCase()
}

type Props = {
  name: string
  /** A feltöltött logó URL-je; ha nincs, a névből képzett monogram jelenik meg. */
  logoUrl?: string | null
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

/**
 * Csoport-logó: a feltöltött kép, vagy egy névből képzett, determinisztikus színű monogram.
 * Ugyanazt a kör-megjelenítést használja, mint az {@link Avatar} (a felhasználói profilképekhez
 * illesztve), csak a monogram-szabály csoport-specifikus.
 */
export default function GroupLogo({ name, logoUrl, size = 'md', className = '' }: Props) {
  return <Avatar name={name} src={logoUrl} initials={groupInitials(name)} size={size} className={className} />
}
