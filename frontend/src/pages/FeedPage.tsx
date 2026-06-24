import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { CheckCircle2, Rss } from 'lucide-react'
import PostComposer from '../components/PostComposer'

export default function FeedPage() {
  const { t } = useTranslation()
  // A hírfolyam (#10) csak ismerős + követett + csoport posztokat mutat, ezért a saját
  // új bejegyzés itt nem jelenik meg — visszajelzésként a profilra mutató linket adunk.
  const [posted, setPosted] = useState(false)

  return (
    <div className="flex flex-col gap-4">
      <PostComposer onCreated={() => setPosted(true)} />

      {posted && (
        <div
          className="flex items-center gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700"
          role="status"
        >
          <CheckCircle2 className="h-4 w-4 shrink-0" />
          <span>
            {t('composer.postedFeed')}{' '}
            <Link to="/profile" className="font-semibold underline">
              {t('composer.viewOnProfile')}
            </Link>
          </span>
        </div>
      )}

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
