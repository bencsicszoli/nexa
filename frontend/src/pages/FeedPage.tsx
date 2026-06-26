import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { CheckCircle2, Loader2, Rss } from 'lucide-react'
import PostComposer from '../components/PostComposer'
import PostCard from '../components/PostCard'
import { errorKey } from '../auth/errorKey'
import { getFeed } from '../feed/feedApi'
import type { Post } from '../posts/types'

export default function FeedPage() {
  const { t } = useTranslation()
  // A hírfolyam (#10) csak ismerős + követett + csoport posztokat mutat, ezért a saját
  // új bejegyzés itt nem jelenik meg — visszajelzésként a profilra mutató linket adunk.
  const [posted, setPosted] = useState(false)

  const [posts, setPosts] = useState<Post[]>([])
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Az első lap (újra)betöltése.
  const loadFirstPage = useCallback(() => {
    setLoading(true)
    setError(null)
    getFeed()
      .then((page) => {
        setPosts(page.items)
        setNextCursor(page.nextCursor)
      })
      .catch((err) => setError(errorKey(err)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    getFeed()
      .then((page) => {
        if (!active) return
        setPosts(page.items)
        setNextCursor(page.nextCursor)
      })
      .catch((err) => active && setError(errorKey(err)))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [])

  // A következő lap hozzáfűzése a cursorral (keyset-lapozás).
  async function loadMore() {
    if (!nextCursor || loadingMore) return
    setLoadingMore(true)
    setError(null)
    try {
      const page = await getFeed(nextCursor)
      setPosts((prev) => [...prev, ...page.items])
      setNextCursor(page.nextCursor)
    } catch (err) {
      setError(errorKey(err))
    } finally {
      setLoadingMore(false)
    }
  }

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

      {loading ? (
        <div className="flex justify-center py-16 text-slate-400">
          <Loader2 className="h-6 w-6 animate-spin" />
        </div>
      ) : error && posts.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-2xl border border-rose-200 bg-rose-50 px-6 py-12 text-center">
          <p className="text-sm text-rose-600" role="alert">
            {t(error)}
          </p>
          <button
            type="button"
            onClick={loadFirstPage}
            className="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark"
          >
            {t('feed.retry')}
          </button>
        </div>
      ) : posts.length === 0 ? (
        /* Üres hírfolyam — még nincs ismerős/követett/csoport poszt */
        <section className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center">
          <span className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-brand/10 text-brand">
            <Rss className="h-7 w-7" />
          </span>
          <h2 className="text-base font-semibold text-slate-900">{t('feed.emptyTitle')}</h2>
          <p className="mt-1 max-w-md text-sm text-slate-500">{t('feed.emptySubtitle')}</p>
        </section>
      ) : (
        <>
          {posts.map((post) => (
            <PostCard key={post.id} post={post} showGroupBadge />
          ))}

          {nextCursor && (
            <div className="flex flex-col items-center gap-2 py-2">
              {error && (
                <p className="text-sm text-rose-600" role="alert">
                  {t(error)}
                </p>
              )}
              <button
                type="button"
                onClick={loadMore}
                disabled={loadingMore}
                className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60"
              >
                {loadingMore && <Loader2 className="h-4 w-4 animate-spin" />}
                {loadingMore ? t('feed.loadingMore') : t('feed.loadMore')}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
