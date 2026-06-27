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
import type { NexaNotification } from './types'

/**
 * A valós idejű értesítés (#11) kliensoldali állapota. A megosztott STOMP-kapcsolaton
 * ({@link useStomp}) feliratkozik a saját értesítéseire (`/user/queue/notifications`): ha egy
 * kapcsolat új tartalmat tölt fel, az értesítés azonnal megjelenik — toastként és a fejléc
 * harang-jelvényén. A kapcsolat felépítését/hitelesítését a {@link StompProvider} intézi.
 */
type NotificationsValue = {
  /** A kapott értesítések, legfrissebb elöl (korlátozott számban megtartva). */
  notifications: NexaNotification[]
  /** Az olvasatlan (a harang megnyitása óta érkezett) értesítések száma. */
  unreadCount: number
  /** A legutóbbi értesítés a toasthoz (vagy null, ha nincs/elrejtve). */
  toast: NexaNotification | null
  /** Mindet olvasottnak jelöli (a harang lenyitásakor) → a jelvény eltűnik. */
  markAllRead: () => void
  /** A toast bezárása (kattintás vagy automatikus elrejtés után). */
  dismissToast: () => void
}

const NotificationsContext = createContext<NotificationsValue | null>(null)

/** Legfeljebb ennyi értesítést tartunk meg a listában (a harang lenyíló paneljéhez). */
const MAX_NOTIFICATIONS = 30
/** A toast automatikus elrejtésének ideje (ms). */
const TOAST_TIMEOUT_MS = 8000

export function NotificationsProvider({ children }: { children: ReactNode }) {
  const { subscribe } = useStomp()

  const [notifications, setNotifications] = useState<NexaNotification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [toast, setToast] = useState<NexaNotification | null>(null)

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
        setNotifications((prev) => [dto, ...prev].slice(0, MAX_NOTIFICATIONS))
        setUnreadCount((c) => c + 1)
        setToast(dto)
      } catch {
        // Hibás keret esetén nem teszünk semmit — az értesítés csak kényelmi jelzés.
      }
    })
    return unsubscribe
  }, [subscribe])

  const markAllRead = useCallback(() => setUnreadCount(0), [])
  const dismissToast = useCallback(() => setToast(null), [])

  const value = useMemo<NotificationsValue>(
    () => ({ notifications, unreadCount, toast, markAllRead, dismissToast }),
    [notifications, unreadCount, toast, markAllRead, dismissToast],
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
