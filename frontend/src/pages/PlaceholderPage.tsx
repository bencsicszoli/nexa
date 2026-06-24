import { useTranslation } from 'react-i18next'
import { Construction } from 'lucide-react'

type PlaceholderPageProps = {
  /** A navigációs fordításkulcs (pl. "nav.friends"), amihez ez az oldal tartozik. */
  titleKey: string
}

// Ideiglenes oldal a még meg nem valósított navigációs pontokhoz.
// A vázon belüli navigáció így már most működik és tesztelhető (#2 DoD).
export default function PlaceholderPage({ titleKey }: PlaceholderPageProps) {
  const { t } = useTranslation()
  const section = t(titleKey)

  return (
    <section className="flex flex-col items-center justify-center rounded-2xl border border-slate-200 bg-white px-6 py-20 text-center">
      <span className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-slate-100 text-slate-400">
        <Construction className="h-7 w-7" />
      </span>
      <h1 className="text-lg font-semibold text-slate-900">{section}</h1>
      <p className="mt-1 text-sm text-slate-500">{t('page.comingSoon')}</p>
      <p className="mt-0.5 text-xs text-slate-400">{t('page.comingSoonHint', { section })}</p>
    </section>
  )
}
