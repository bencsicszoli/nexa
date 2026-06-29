import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { Loader2, Search as SearchIcon, Users } from 'lucide-react'
import Avatar from '../components/Avatar'
import GroupLogo from '../components/GroupLogo'
import PostCard from '../components/PostCard'
import { errorKey } from '../auth/errorKey'
import { search } from '../search/searchApi'
import type { SearchResults } from '../search/types'

type Tab = 'all' | 'people' | 'groups' | 'posts'

const EMPTY =
  'rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-10 text-center text-sm text-slate-500'

export default function SearchPage() {
  const { t } = useTranslation()
  const [params] = useSearchParams()
  const query = (params.get('q') ?? '').trim()

  const [tab, setTab] = useState<Tab>('all')
  const [results, setResults] = useState<SearchResults | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // A keresés a TopBar inputjából frissülő ?q paraméterre fut, debounce-olva — így a gyors
  // gépelés nem indít feleslegesen sok hívást.
  useEffect(() => {
    if (!query) {
      setResults(null)
      setError(null)
      setLoading(false)
      return
    }
    let active = true
    setLoading(true)
    const handle = setTimeout(() => {
      search(query)
        .then((res) => {
          if (active) {
            setResults(res)
            setError(null)
          }
        })
        .catch((err) => {
          if (active) setError(errorKey(err))
        })
        .finally(() => {
          if (active) setLoading(false)
        })
    }, 300)
    return () => {
      active = false
      clearTimeout(handle)
    }
  }, [query])

  const counts = useMemo(
    () => ({
      people: results?.users.length ?? 0,
      groups: results?.groups.length ?? 0,
      posts: results?.posts.length ?? 0,
    }),
    [results],
  )
  const total = counts.people + counts.groups + counts.posts

  const tabs: { id: Tab; label: string; badge?: number }[] = [
    { id: 'all', label: t('search.tab.all'), badge: total || undefined },
    { id: 'people', label: t('search.tab.people'), badge: counts.people || undefined },
    { id: 'groups', label: t('search.tab.groups'), badge: counts.groups || undefined },
    { id: 'posts', label: t('search.tab.posts'), badge: counts.posts || undefined },
  ]

  const showPeople = tab === 'all' || tab === 'people'
  const showGroups = tab === 'all' || tab === 'groups'
  const showPosts = tab === 'all' || tab === 'posts'

  return (
    <div className="flex flex-col gap-4">
      <header className="rounded-2xl border border-slate-200 bg-white p-6">
        <h1 className="text-lg font-semibold text-slate-900">{t('search.title')}</h1>
        <p className="mt-0.5 text-sm text-slate-500">
          {query ? t('search.resultsFor', { query }) : t('search.subtitle')}
        </p>

        <div className="mt-4 flex gap-1 overflow-x-auto border-b border-slate-200">
          {tabs.map((tb) => (
            <button
              key={tb.id}
              type="button"
              onClick={() => setTab(tb.id)}
              className={`-mb-px flex shrink-0 items-center gap-2 border-b-2 px-3 py-2 text-sm font-medium transition-colors ${
                tab === tb.id
                  ? 'border-brand text-brand'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              {tb.label}
              {tb.badge != null && (
                <span className="rounded-full bg-brand/10 px-1.5 text-xs font-semibold text-brand">
                  {tb.badge}
                </span>
              )}
            </button>
          ))}
        </div>
      </header>

      {error && (
        <p className={`${EMPTY} border-rose-200 bg-rose-50 text-rose-600`} role="alert">
          {t(error)}
        </p>
      )}

      {!query ? (
        <p className={EMPTY}>
          <SearchIcon className="mx-auto mb-2 h-5 w-5 text-slate-400" />
          {t('search.empty')}
        </p>
      ) : loading && !results ? (
        <div className="flex justify-center py-12 text-slate-400">
          <Loader2 className="h-5 w-5 animate-spin" />
        </div>
      ) : results && total === 0 ? (
        <p className={EMPTY}>{t('search.noResults', { query })}</p>
      ) : results ? (
        <div className="flex flex-col gap-6">
          {/* Emberek */}
          {showPeople && counts.people > 0 && (
            <section className="flex flex-col gap-2">
              {tab === 'all' && <SectionTitle>{t('search.tab.people')}</SectionTitle>}
              <ul className="flex flex-col gap-2">
                {results.users.map((u) => (
                  <li key={u.id}>
                    <Link
                      to={`/users/${u.id}`}
                      className="group flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3"
                    >
                      <Avatar name={u.displayName} src={u.avatarUrl} size="lg" />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-semibold text-slate-900 group-hover:text-brand">
                          {u.displayName}
                        </p>
                        {u.bio && <p className="truncate text-xs text-slate-500">{u.bio}</p>}
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {/* Csoportok */}
          {showGroups && counts.groups > 0 && (
            <section className="flex flex-col gap-2">
              {tab === 'all' && <SectionTitle>{t('search.tab.groups')}</SectionTitle>}
              <ul className="flex flex-col gap-2">
                {results.groups.map((g) => (
                  <li key={g.id}>
                    <Link
                      to={`/groups/${g.id}`}
                      className="group flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3"
                    >
                      <GroupLogo name={g.name} logoUrl={g.logoUrl} size="lg" className="h-11 w-11" />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-semibold text-slate-900 group-hover:text-brand">
                          {g.name}
                        </p>
                        <p className="flex items-center gap-1 truncate text-xs text-slate-500">
                          <Users className="h-3.5 w-3.5" />
                          {t('groups.memberCount', { count: g.memberCount })}
                          {g.description && (
                            <>
                              <span aria-hidden="true">·</span>
                              <span className="truncate">{g.description}</span>
                            </>
                          )}
                        </p>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {/* Bejegyzések */}
          {showPosts && counts.posts > 0 && (
            <section className="flex flex-col gap-2">
              {tab === 'all' && <SectionTitle>{t('search.tab.posts')}</SectionTitle>}
              <div className="flex flex-col gap-3">
                {results.posts.map((p) => (
                  <PostCard key={p.id} post={p} showGroupBadge />
                ))}
              </div>
            </section>
          )}

          {/* A kiválasztott fül üres, de más típusra van találat */}
          {tab !== 'all' &&
            ((tab === 'people' && counts.people === 0) ||
              (tab === 'groups' && counts.groups === 0) ||
              (tab === 'posts' && counts.posts === 0)) && (
              <p className={EMPTY}>{t('search.noResultsType')}</p>
            )}
        </div>
      ) : null}
    </div>
  )
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="px-1 text-xs font-semibold uppercase tracking-wide text-slate-400">{children}</h2>
  )
}
