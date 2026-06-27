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
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { useAuth } from '../auth/AuthContext'
import { accessTokenExpiringWithin, getAccessToken } from '../auth/tokenStore'
import { refreshTokens } from '../lib/api'

/**
 * Egyetlen, megosztott STOMP-kapcsolat a bejelentkezett felhasználóhoz (#11 + #12). Korábban
 * az értesítés saját kapcsolatot tartott; mióta a csevegés is valós idejű, egy kapcsolaton
 * osztozunk — minden funkció (értesítés, üzenet, gépelés, jelenlét) ennek a providernek a
 * `subscribe`/`publish` API-ját használja.
 *
 * A kapcsolat hitelesítése a STOMP CONNECT `Authorization: Bearer <token>` fejlécével történik
 * (a böngésző a WebSocket-kézfogásnál nem tud fejlécet küldeni). A `beforeConnect` minden
 * (újra)csatlakozás előtt fut: ha az access token lejárt vagy hamarosan lejár, előbb **frissíti**
 * (az `apiFetch` single-flight refresh-ét újrahasználva), és csak utána teszi a friss tokent a
 * fejlécbe. Enélkül egy 15 perces lejárat utáni reconnect (hálózati szünet, alvás, szerver-restart)
 * lejárt tokennel próbálkozna a végtelenségig, és a realtime-csatorna (értesítés + chat) elnémulna.
 * A feliratkozásokat a provider tartja nyilván, és minden (újra)csatlakozáskor helyreállítja őket.
 */
type MessageHandler = (message: IMessage) => void

type StompContextValue = {
  /** Igaz, ha a STOMP-kapcsolat él (a feliratkozások ettől függetlenül megmaradnak). */
  connected: boolean
  /** Feliratkozás egy célra; a visszaadott függvény leiratkozik. */
  subscribe: (destination: string, handler: MessageHandler) => () => void
  /** Üzenet küldése egy célra (JSON-ként). Ha nincs élő kapcsolat, csendben eldobjuk. */
  publish: (destination: string, body: unknown) => void
}

const StompContext = createContext<StompContextValue | null>(null)

const WS_URL = `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`

type Desire = {
  destination: string
  handler: MessageHandler
  sub: StompSubscription | null
}

export function StompProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const userId = user?.id ?? null

  const [connected, setConnected] = useState(false)
  const clientRef = useRef<Client | null>(null)
  // A kívánt feliratkozások id → leírás; a tényleges STOMP-subscription a `sub`.
  const desiresRef = useRef<Map<number, Desire>>(new Map())
  const idCounterRef = useRef(0)

  useEffect(() => {
    if (!userId) return

    const client = new Client({
      brokerURL: WS_URL,
      // Async: lejárt/hamarosan lejáró access tokent (újra)csatlakozás előtt frissítünk, hogy a
      // CONNECT mindig érvényes tokent kapjon. Refresh-bukásnál a (lejárt) tokennel próbálkozunk —
      // a kijelentkeztetést úgyis a normál apiFetch-folyam intézi.
      beforeConnect: async () => {
        if (accessTokenExpiringWithin(30_000)) {
          await refreshTokens()
        }
        const token = getAccessToken()
        client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {}
      },
      reconnectDelay: 5000,
      onConnect: () => {
        // (Újra)csatlakozáskor minden kívánt feliratkozást helyreállítunk; a korábbi
        // subscription-handle-ök egy bontás után érvénytelenek.
        for (const desire of desiresRef.current.values()) {
          desire.sub = client.subscribe(desire.destination, desire.handler)
        }
        setConnected(true)
      },
      onWebSocketClose: () => {
        for (const desire of desiresRef.current.values()) {
          desire.sub = null
        }
        setConnected(false)
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      clientRef.current = null
      for (const desire of desiresRef.current.values()) {
        desire.sub = null
      }
      void client.deactivate()
      setConnected(false)
    }
  }, [userId])

  const subscribe = useCallback((destination: string, handler: MessageHandler) => {
    const id = ++idCounterRef.current
    const desire: Desire = { destination, handler, sub: null }
    desiresRef.current.set(id, desire)

    const client = clientRef.current
    if (client && client.connected) {
      desire.sub = client.subscribe(destination, handler)
    }

    return () => {
      const existing = desiresRef.current.get(id)
      if (existing?.sub) {
        try {
          existing.sub.unsubscribe()
        } catch {
          // A kapcsolat közben bonthatott — a leiratkozás ilyenkor lényegtelen.
        }
      }
      desiresRef.current.delete(id)
    }
  }, [])

  const publish = useCallback((destination: string, body: unknown) => {
    const client = clientRef.current
    if (client && client.connected) {
      client.publish({ destination, body: JSON.stringify(body) })
    }
  }, [])

  const value = useMemo<StompContextValue>(
    () => ({ connected, subscribe, publish }),
    [connected, subscribe, publish],
  )

  return <StompContext.Provider value={value}>{children}</StompContext.Provider>
}

export function useStomp(): StompContextValue {
  const ctx = useContext(StompContext)
  if (!ctx) {
    throw new Error('useStomp csak StompProvider-en belül használható')
  }
  return ctx
}
