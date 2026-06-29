import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { useStomp } from '../realtime/StompProvider'
import {
  getNotifications,
  getUnreadCount,
  markAllRead as apiMarkAllRead,
} from './notificationApi'
import type { NexaNotification } from './types'

/**
 * Az értesítések (#11, #17) kliensoldali állapota. Mountkor a szerverről tölti az előzményt és az
 * olvasatlan-számot (így újratöltés után is megmarad), majd a megosztott STOMP-kapcsolaton
 * ({@link useStomp}) feliratkozik a saját értesítéseire (`/user/queue/notifications`): új esemény
 * azonnal megjelenik — toastként és a harang-jelvényen. A push előre szúr, id szerint deduplikálva.
 */
type NotificationsValue = {
  /** A legutóbbi értesítések, legfrissebb elöl (a harang lenyíló paneljéhez). */
  notifications: NexaNotification[]
  /** Az olvasatlan értesítések száma (a szerverrel szinkronban). */
  unreadCount: number
  /** A legutóbbi push-értesítés a toasthoz (vagy null). */
  toast: NexaNotification | null
  /** Mindet olvasottnak jelöli a szerveren is → a jelvény eltűnik. */
  markAllRead: () => void
  /** Az olvasatlan-szám újratöltése a szerverről (pl. a /notifications oldal műveletei után). */
  refreshUnread: () => void
  /** A toast bezárása. */
  dismissToast: () => void
}

const NotificationsContext = createContext<NotificationsValue | null>(null)

/** Legfeljebb ennyi értesítést tartunk meg a harang-listában. */
const MAX_NOTIFICATIONS = 30
/** A toast automatikus elrejtésének ideje (ms). */
const TOAST_TIMEOUT_MS = 8000

export function NotificationsProvider({ children }: { children: ReactNode }) {
  const { subscribe } = useStomp()

  const [notifications, setNotifications] = useState<NexaNotification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [toast, setToast] = useState<NexaNotification | null>(null)

  // Mountkor: előzmény + olvasatlan-szám a szerverről (best-effort).
  useEffect(() => {
    let active = true
    getNotifications(0)
      .then((page) => {
        if (active) setNotifications(page.items)
      })
      .catch(() => {})
    getUnreadCount()
      .then((count) => {
        if (active) setUnreadCount(count)
      })
      .catch(() => {})
    return () => {
      active = false
    }
  }, [])

  // A toast automatikus elrejtése.
  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(null), TOAST_TIMEOUT_MS)
    return () => window.clearTimeout(timer)
  }, [toast])

  // Feliratkozás a saját értesítésekre a megosztott STOMP-kapcsolaton.
  useEffect(() => {
    const unsubscribe = subscribe('/user/queue/notifications', (frame) => {
      try {
        const dto = JSON.parse(frame.body) as NexaNotification
        setNotifications((prev) => {
          if (prev.some((n) => n.id === dto.id)) return prev // dedup
          return [dto, ...prev].slice(0, MAX_NOTIFICATIONS)
        })
        setUnreadCount((c) => c + 1)
        setToast(dto)
      } catch {
        // Hibás keret esetén nem teszünk semmit — az értesítés csak kényelmi jelzés.
      }
    })
    return unsubscribe
  }, [subscribe])

  const markAllRead = useCallback(() => {
    setUnreadCount(0)
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
    apiMarkAllRead().catch(() => {})
  }, [])

  const refreshUnread = useCallback(() => {
    getUnreadCount()
      .then(setUnreadCount)
      .catch(() => {})
  }, [])

  const dismissToast = useCallback(() => setToast(null), [])

  const value = useMemo<NotificationsValue>(
    () => ({ notifications, unreadCount, toast, markAllRead, refreshUnread, dismissToast }),
    [notifications, unreadCount, toast, markAllRead, refreshUnread, dismissToast],
  )

  return <NotificationsContext.Provider value={value}>{children}</NotificationsContext.Provider>
}

export function useNotifications(): NotificationsValue {
  const ctx = useContext(NotificationsContext)
  if (!ctx) {
    throw new Error('useNotifications csak NotificationsProvider-en belül használható')
  }
  return ctx
}
