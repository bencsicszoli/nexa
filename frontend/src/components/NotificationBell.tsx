import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Bell } from 'lucide-react'
import Avatar from './Avatar'
import { useNotifications } from '../notifications/NotificationsContext'
import { useOpenNotification } from '../notifications/useOpenNotification'
import { notificationText } from '../notifications/notificationText'
import { formatRelativeTime } from '../lib/time'
import type { NexaNotification } from '../notifications/types'

/**
 * Fejléc-harang az értesítésekkel (#11, #17). A jelvény az olvasatlan értesítések számát
 * mutatja; lenyitva a legutóbbiak listája jelenik meg, és mind olvasottá válik (a jelvény
 * eltűnik). Egy tételre kattintva a típusának megfelelő helyre navigál; alul „Összes megtekintése".
 */
export default function NotificationBell() {
  const { t, i18n } = useTranslation()
  const { notifications, unreadCount, markAllRead } = useNotifications()
  const open = useOpenNotification()
  const navigate = useNavigate()

  const [panelOpen, setPanelOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  // Lenyitáskor mindet olvasottá tesszük → a jelvény eltűnik.
  useEffect(() => {
    if (panelOpen) markAllRead()
  }, [panelOpen, markAllRead])

  // Kattintás a panelen kívülre → bezárás.
  useEffect(() => {
    if (!panelOpen) return
    const onClick = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setPanelOpen(false)
      }
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [panelOpen])

  const handleItem = (n: NexaNotification) => {
    open(n)
    setPanelOpen(false)
  }

  const handleSeeAll = () => {
    navigate('/notifications')
    setPanelOpen(false)
  }

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => setPanelOpen((v) => !v)}
        className="relative rounded-full p-2 text-slate-600 hover:bg-slate-100"
        aria-label={t('topbar.notifications')}
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute right-1.5 top-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {panelOpen && (
        <div className="absolute right-0 z-50 mt-2 w-80 max-w-[calc(100vw-1.5rem)] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
          <div className="border-b border-slate-100 px-4 py-2.5">
            <h2 className="text-sm font-semibold text-slate-900">{t('topbar.notifications')}</h2>
          </div>

          {notifications.length === 0 ? (
            <p className="px-4 py-8 text-center text-sm text-slate-500">
              {t('notifications.empty')}
            </p>
          ) : (
            <ul className="max-h-96 divide-y divide-slate-100 overflow-y-auto">
              {notifications.map((n) => (
                <li key={n.id}>
                  <button
                    type="button"
                    onClick={() => handleItem(n)}
                    className="flex w-full items-start gap-3 px-4 py-3 text-left hover:bg-slate-50"
                  >
                    <Avatar name={n.actorName} src={n.actorAvatarUrl} size="sm" />
                    <span className="min-w-0 flex-1">
                      <span className="block text-sm text-slate-800">
                        {notificationText(n, t)}
                      </span>
                      <span className="mt-0.5 block text-xs text-slate-400">
                        {formatRelativeTime(n.createdAt, i18n.language)}
                      </span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}

          <button
            type="button"
            onClick={handleSeeAll}
            className="block w-full border-t border-slate-100 px-4 py-2.5 text-center text-sm font-medium text-brand hover:bg-slate-50"
          >
            {t('notifications.seeAll')}
          </button>
        </div>
      )}
    </div>
  )
}
