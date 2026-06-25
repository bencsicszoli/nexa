/**
 * Emberbarát időbélyeg: friss eseménynél relatív („3 perce"), régebbinél lokalizált dátum.
 * A poszt- és komment-időpontok közös formázója.
 */
export function formatRelativeTime(iso: string, lang: string): string {
  const date = new Date(iso)
  const diffMs = Date.now() - date.getTime()
  const diffSec = Math.round(diffMs / 1000)
  const rtf = new Intl.RelativeTimeFormat(lang, { numeric: 'auto' })

  if (diffSec < 60) return rtf.format(-diffSec, 'second')
  const diffMin = Math.round(diffSec / 60)
  if (diffMin < 60) return rtf.format(-diffMin, 'minute')
  const diffHour = Math.round(diffMin / 60)
  if (diffHour < 24) return rtf.format(-diffHour, 'hour')
  const diffDay = Math.round(diffHour / 24)
  if (diffDay < 7) return rtf.format(-diffDay, 'day')

  return date.toLocaleDateString(lang, { year: 'numeric', month: 'short', day: 'numeric' })
}
