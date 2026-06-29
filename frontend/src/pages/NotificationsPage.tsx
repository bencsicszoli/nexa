import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CheckCheck, Loader2 } from 'lucide-react'
import Avatar from '../components/Avatar'
import { useNotifications } from '../notifications/NotificationsContext'
import { useOpenNotification } from '../notifications/useOpenNotification'
import { notificationText } from '../notifications/notificationText'
import {
  getNotifications,
  markAllRead as apiMarkAllRead,
  markRead as apiMarkRead,
} from '../notifications/notificationApi'
import type { NexaNotification } from '../notifications/types'
import { formatRelativeTime } from '../lib/time'

/**
 * Az értesítési központ teljes előzménye (#17), lapozással. Egy tételre kattintva a típusának
 * megfelelő helyre navigál és olvasottá teszi; „Mind olvasott" gombbal egyszerre is. A harang
 * jelvénye a {@link useNotifications}-on keresztül szinkronban marad.
 */
export default function NotificationsPage() {
  const { t, i18n } = useTranslation()
  const open = useOpenNotification()
  const { refreshUnread } = useNotifications()

  const [items, setItems] = useState<NexaNotification[]>([])
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState(false)

  const loadFirst = useCallback(() => {
    setLoading(true)
    setError(false)
    getNotifications(0)
      .then((p) => {
        setItems(p.items)
        setPage(0)
        setHasMore(p.hasMore)
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    loadFirst()
  }, [loadFirst])

  const loadMore = () => {
    setLoadingMore(true)
    getNotifications(page + 1)
      .then((p) => {
        setItems((prev) => [...prev, ...p.items])
        setPage(p.page)
        setHasMore(p.hasMore)
      })
      .catch(() => {})
      .finally(() => setLoadingMore(false))
  }

  const onItem = (n: NexaNotification) => {
    if (!n.read) {
      setItems((prev) => prev.map((x) => (x.id === n.id ? { ...x, read: true } : x)))
      apiMarkRead(n.id)
        .then(refreshUnread)
        .catch(() => {})
    }
    open(n)
  }

  const onMarkAll = () => {
    setItems((prev) => prev.map((x) => ({ ...x, read: true })))
    apiMarkAllRead()
      .then(refreshUnread)
      .catch(() => {})
  }

  return (
    <div className="flex flex-col gap-4">
      <header className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white p-6">
        <div>
          <h1 className="text-lg font-semibold text-slate-900">{t('notifications.title')}</h1>
          <p className="mt-0.5 text-sm text-slate-500">{t('notifications.subtitle')}</p>
        </div>
        {items.some((n) => !n.read) && (
          <button
            type="button"
            onClick={onMarkAll}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100"
          >
            <CheckCheck className="h-4 w-4" />
            {t('notifications.markAllRead')}
          </button>
        )}
      </header>

      {loading ? (
        <div className="flex justify-center py-10 text-slate-400">
          <Loader2 className="h-6 w-6 animate-spin" />
        </div>
      ) : error ? (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-6 text-center" role="alert">
          <p className="text-sm text-rose-600">{t('notifications.loadError')}</p>
          <button
            type="button"
            onClick={loadFirst}
            className="mt-2 text-sm font-semibold text-brand hover:underline"
          >
            {t('notifications.retry')}
          </button>
        </div>
      ) : items.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-10 text-center text-sm text-slate-500">
          {t('notifications.empty')}
        </p>
      ) : (
        <ul className="divide-y divide-slate-100 overflow-hidden rounded-2xl border border-slate-200 bg-white">
          {items.map((n) => (
            <li key={n.id}>
              <button
                type="button"
                onClick={() => onItem(n)}
                className={`flex w-full items-start gap-3 px-4 py-3.5 text-left transition-colors hover:bg-slate-50 ${
                  n.read ? '' : 'bg-brand/5'
                }`}
              >
                <Avatar name={n.actorName} src={n.actorAvatarUrl} size="sm" />
                <span className="min-w-0 flex-1">
                  <span className="block text-sm text-slate-800">{notificationText(n, t)}</span>
                  <span className="mt-0.5 block text-xs text-slate-400">
                    {formatRelativeTime(n.createdAt, i18n.language)}
                  </span>
                </span>
                {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-brand" />}
              </button>
            </li>
          ))}
        </ul>
      )}

      {!loading && !error && hasMore && (
        <button
          type="button"
          onClick={loadMore}
          disabled={loadingMore}
          className="mx-auto inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60"
        >
          {loadingMore && <Loader2 className="h-4 w-4 animate-spin" />}
          {loadingMore ? t('notifications.loadingMore') : t('notifications.loadMore')}
        </button>
      )}
    </div>
  )
}
