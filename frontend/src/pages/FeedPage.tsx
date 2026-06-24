import { useTranslation } from 'react-i18next'
import { Image as ImageIcon, PenLine, Rss, Video } from 'lucide-react'
import Avatar from '../components/Avatar'
import { useAuth } from '../auth/AuthContext'

export default function FeedPage() {
  const { t } = useTranslation()
  const { user } = useAuth()

  return (
    <div className="flex flex-col gap-4">
      {/* Szerkesztődoboz — a valódi posztolást az #5/#6 kártya köti be */}
      <section className="rounded-2xl border border-slate-200 bg-white p-4">
        <div className="flex items-center gap-3">
          <Avatar name={user?.displayName ?? 'Nexa'} size="md" />
          <input
            type="text"
            placeholder={t('composer.placeholder')}
            className="w-full rounded-full border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none transition-colors placeholder:text-slate-400 focus:border-brand focus:bg-white"
          />
        </div>
        <div className="mt-3 flex items-center justify-around border-t border-slate-100 pt-3">
          <button
            type="button"
            className="flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100"
          >
            <ImageIcon className="h-4 w-4 text-emerald-500" />
            {t('composer.photo')}
          </button>
          <button
            type="button"
            className="flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100"
          >
            <Video className="h-4 w-4 text-rose-500" />
            {t('composer.video')}
          </button>
          <button
            type="button"
            className="flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100"
          >
            <PenLine className="h-4 w-4 text-brand" />
            {t('composer.text')}
          </button>
        </div>
      </section>

      {/* Üres hírfolyam-placeholder — a #10 kártya tölti fel valós posztokkal */}
      <section className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center">
        <span className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-brand/10 text-brand">
          <Rss className="h-7 w-7" />
        </span>
        <h2 className="text-base font-semibold text-slate-900">{t('feed.emptyTitle')}</h2>
        <p className="mt-1 max-w-md text-sm text-slate-500">{t('feed.emptySubtitle')}</p>
      </section>
    </div>
  )
}
