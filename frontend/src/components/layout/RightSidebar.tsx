import { useTranslation } from 'react-i18next'
import { Sparkles, X } from 'lucide-react'
import Avatar from '../Avatar'

// Placeholder adat — a valódi tartalmat a kapcsolati/értesítési kártyák töltik fel
// (#7 ismerősök, #8 követés, #11 értesítés, #14 előfizetés).
const FRIENDS = ['Anna Kovács', 'Béla Nagy', 'Csaba Tóth', 'Dóra Szabó']
const CREATORS = [
  { name: 'Tech Híradó', meta: 'napi 3 poszt' },
  { name: 'Könyvklub', meta: 'heti összefoglaló' },
]

function ListCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4">
      <h2 className="mb-3 text-sm font-semibold text-slate-900">{title}</h2>
      <div className="flex flex-col gap-3">{children}</div>
    </section>
  )
}

export default function RightSidebar() {
  const { t } = useTranslation()

  return (
    <div className="flex flex-col gap-4">
      {/* Valós idejű értesítés-előnézet (statikus váz; élővé a #11 kártya teszi) */}
      <div className="flex items-start gap-3 rounded-2xl bg-slate-900 p-4 text-white">
        <Avatar name="Anna Kovács" size="sm" />
        <div className="min-w-0 flex-1">
          <p className="text-sm">{t('right.newPostToast', { name: 'Anna Kovács' })}</p>
          <p className="mt-0.5 text-xs text-slate-300">{t('right.newPostHint')}</p>
        </div>
        <X className="h-4 w-4 shrink-0 cursor-pointer text-slate-400 hover:text-white" />
      </div>

      <ListCard title={t('right.friends')}>
        {FRIENDS.map((name) => (
          <div key={name} className="flex items-center gap-3">
            <Avatar name={name} size="sm" />
            <span className="truncate text-sm text-slate-700">{name}</span>
          </div>
        ))}
      </ListCard>

      <ListCard title={t('right.followedCreators')}>
        {CREATORS.map((c) => (
          <div key={c.name} className="flex items-center gap-3">
            <Avatar name={c.name} size="sm" />
            <div className="min-w-0">
              <div className="truncate text-sm font-medium text-slate-700">{c.name}</div>
              <div className="truncate text-xs text-slate-400">{c.meta}</div>
            </div>
          </div>
        ))}
      </ListCard>

      {/* Előfizetés-kártya (a #14 kártya köti be a Paddle-höz) */}
      <section className="rounded-2xl border border-brand/20 bg-brand/5 p-4">
        <div className="flex items-center gap-2 text-sm font-semibold text-brand">
          <Sparkles className="h-4 w-4" />
          {t('right.premiumTitle')}
        </div>
        <p className="mt-2 text-xs text-slate-600">
          {t('right.premiumDesc')}{' '}
          <span className="font-semibold text-slate-800">
            {t('right.premiumTrial', { days: 9 })}
          </span>
        </p>
        <button
          type="button"
          className="mt-3 w-full rounded-lg bg-brand py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark"
        >
          {t('right.premiumManage')}
        </button>
      </section>
    </div>
  )
}
