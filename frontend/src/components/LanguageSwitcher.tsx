import { useTranslation } from 'react-i18next'
import type { Language } from '../i18n'

// A makett szerinti "HU / EN" kapcsoló. A választást az i18next a
// localStorage-ba menti (lásd i18n.ts), így újratöltés után is megmarad.
export default function LanguageSwitcher() {
  const { i18n, t } = useTranslation()
  const current = (i18n.resolvedLanguage ?? 'hu') as Language

  const langs: Language[] = ['hu', 'en']

  return (
    <div
      className="flex items-center rounded-full border border-slate-200 bg-white p-0.5 text-xs font-semibold"
      role="group"
      aria-label={t('topbar.notifications')}
    >
      {langs.map((lng) => {
        const active = current === lng
        return (
          <button
            key={lng}
            type="button"
            onClick={() => void i18n.changeLanguage(lng)}
            aria-pressed={active}
            title={t('lang.switchTo', { lang: t(`lang.${lng}`) })}
            className={`rounded-full px-2.5 py-1 uppercase transition-colors ${
              active
                ? 'bg-brand text-white'
                : 'text-slate-500 hover:text-brand'
            }`}
          >
            {lng}
          </button>
        )
      })}
    </div>
  )
}
