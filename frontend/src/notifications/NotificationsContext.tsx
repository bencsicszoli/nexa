import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { Client } from '@stomp/stompjs'
import { useAuth } from '../auth/AuthContext'
import { getAccessToken } from '../auth/tokenStore'
import type { NexaNotification } from './types'

/**
 * A valós idejű értesítés (#11) kliensoldali állapota. Bejelentkezett felhasználónál egy
 * STOMP-kapcsolatot tart fenn a backenddel (a Vite proxyn át a `/ws`-re), és feliratkozik
 * a saját értesítéseire (`/user/queue/notifications`). Ha egy kapcsolat új tartalmat tölt
 * fel, az értesítés azonnal megjelenik — toastként és a fejléc harang-jelvényén.
 *
 * A kapcsolat hitelesítése a STOMP CONNECT `Authorization: Bearer <token>` fejlécével
 * történik (a böngésző a WebSocket-kézfogásnál nem tud fejlécet küldeni). Újracsatlakozáskor
 * a `beforeConnect` mindig a friss access tokent teszi be.
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

const WS_URL = `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`

export function NotificationsProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const userId = user?.id ?? null

  const [notifications, setNotifications] = useState<NexaNotification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [toast, setToast] = useState<NexaNotification | null>(null)

  // A toast automatikus elrejtése.
  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(null), TOAST_TIMEOUT_MS)
    return () => window.clearTimeout(timer)
  }, [toast])

  // STOMP-kapcsolat felépítése/bontása a bejelentkezett felhasználóhoz kötve.
  const clientRef = useRef<Client | null>(null)
  useEffect(() => {
    if (!userId) return

    const client = new Client({
      brokerURL: WS_URL,
      // Minden (újra)csatlakozás előtt a friss access tokent tesszük a CONNECT fejlécbe.
      beforeConnect: () => {
        const token = getAccessToken()
        client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {}
      },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (frame) => {
          try {
            const dto = JSON.parse(frame.body) as NexaNotification
            setNotifications((prev) => [dto, ...prev].slice(0, MAX_NOTIFICATIONS))
            setUnreadCount((c) => c + 1)
            setToast(dto)
          } catch {
            // Hibás keret esetén nem teszünk semmit — az értesítés csak kényelmi jelzés.
          }
        })
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      clientRef.current = null
      // A deactivate aszinkron lebontja a kapcsolatot; a kijelentkezést/újratöltést is ez kezeli.
      void client.deactivate()
      setNotifications([])
      setUnreadCount(0)
      setToast(null)
    }
  }, [userId])

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
