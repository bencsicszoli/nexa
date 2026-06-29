import { useTranslation } from 'react-i18next'
import { X } from 'lucide-react'
import Avatar from './Avatar'
import { useNotifications } from '../notifications/NotificationsContext'
import { useOpenNotification } from '../notifications/useOpenNotification'
import { notificationText } from '../notifications/notificationText'

/**
 * A legutóbbi valós idejű értesítés toastja (jobb alsó sarok). Akkor jelenik meg, ha egy
 * kapcsolat új bejegyzést tölt fel; rákattintva a hírfolyam frissül és az új poszt felül
 * megjelenik. Automatikusan elrejtődik, vagy az „×"-szel bezárható.
 */
export default function NotificationToaster() {
  const { t } = useTranslation()
  const { toast, dismissToast } = useNotifications()
  const open = useOpenNotification()

  if (!toast) return null

  const handleOpen = () => {
    open(toast)
    dismissToast()
  }

  return (
    <div className="fixed bottom-4 right-4 z-50 w-[calc(100%-2rem)] max-w-sm">
      <div className="flex items-start gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-lg">
        <button
          type="button"
          onClick={handleOpen}
          className="flex min-w-0 flex-1 items-start gap-3 text-left"
        >
          <Avatar name={toast.actorName} src={toast.actorAvatarUrl} size="sm" />
          <span className="min-w-0">
            <span className="block truncate text-sm text-slate-900">
              {notificationText(toast, t)}
            </span>
            <span className="mt-0.5 block text-xs font-medium text-brand">
              {t('notifications.clickToOpen')}
            </span>
          </span>
        </button>
        <button
          type="button"
          onClick={dismissToast}
          className="shrink-0 rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
          aria-label={t('notifications.dismiss')}
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}
