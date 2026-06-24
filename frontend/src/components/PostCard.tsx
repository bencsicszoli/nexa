import { useTranslation } from 'react-i18next'
import Avatar from './Avatar'
import type { Post, PostMedia } from '../posts/types'

/** Emberbarát időbélyeg: friss posztoknál relatív, régebbinél lokalizált dátum. */
function formatTime(iso: string, lang: string): string {
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

/** A poszthoz csatolt média rácsa: képek és beágyazott videolejátszó. */
function MediaGrid({ media }: { media: PostMedia[] }) {
  if (media.length === 0) return null
  // Egy elem teljes szélességben; több elem két oszlopos rácsban.
  const cols = media.length === 1 ? 'grid-cols-1' : 'grid-cols-2'

  return (
    <div className={`mt-3 grid gap-2 ${cols}`}>
      {media.map((m, i) =>
        m.type === 'VIDEO' ? (
          <video
            key={i}
            src={m.url}
            controls
            preload="metadata"
            className="max-h-[28rem] w-full rounded-xl border border-slate-200 bg-black"
          />
        ) : (
          <a
            key={i}
            href={m.url}
            target="_blank"
            rel="noreferrer"
            className="block overflow-hidden rounded-xl border border-slate-200"
          >
            <img
              src={m.url}
              alt=""
              loading="lazy"
              className="max-h-[28rem] w-full object-cover"
            />
          </a>
        ),
      )}
    </div>
  )
}

export default function PostCard({ post }: { post: Post }) {
  const { i18n } = useTranslation()

  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-4">
      <header className="flex items-center gap-3">
        <Avatar name={post.authorName} src={post.authorAvatarUrl} size="md" />
        <div className="flex flex-col">
          <span className="text-sm font-semibold text-slate-900">{post.authorName}</span>
          <time
            dateTime={post.createdAt}
            title={new Date(post.createdAt).toLocaleString(i18n.language)}
            className="text-xs text-slate-400"
          >
            {formatTime(post.createdAt, i18n.language)}
          </time>
        </div>
      </header>
      {/* whitespace-pre-wrap: a felhasználó sortörései és szóközei megmaradnak */}
      {post.content && (
        <p className="mt-3 whitespace-pre-wrap break-words text-sm text-slate-700">
          {post.content}
        </p>
      )}
      <MediaGrid media={post.media} />
    </article>
  )
}
