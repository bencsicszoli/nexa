import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { AUTH_LOGOUT_EVENT } from '../lib/api'
import { useAuth } from '../auth/AuthContext'
import { getRequests } from './friendsApi'

/**
 * A bal navigáció „Ismerősök" badge-éhez tartozó állapot: hány **megtekintetlen**
 * beérkezett ismerőskérés van. A jelzés a felhasználó figyelmét hívja fel; nem a függőben
 * lévők darabszáma (azt a /friends „Kérések" fül mutatja).
 *
 * Viselkedés (egyeztetett):
 *  - belépéskor / oldalbetöltéskor egyszer lekérjük a beérkezett kéréseket,
 *  - a /friends megnyitásakor a látott kéréseket „látottá" jelöljük → a badge eltűnik,
 *    akkor is, ha a felhasználó nem reagál rájuk,
 *  - a „látott" halmaz felhasználónként a localStorage-ban él, így újratöltés után sem
 *    jön vissza a badge ugyanarra a kérésre — csak új kérés hozza vissza.
 *
 * A teljes valós idejű frissítés (push) a #11 kártya feladata; itt szándékosan nincs polling.
 */
type FriendNotificationsValue = {
  /** Megtekintetlen beérkezett kérések száma (0 = nincs badge). */
  unseenCount: number
  /**
   * Az aktuális beérkezett kérések azonosítóit adja át: ezeket látottá teszi, és a badge
   * állapotát is ezekhez igazítja. A /friends oldal hívja a lista (újra)betöltésekor és
   * minden elfogadás/elutasítás után.
   */
  markSeen: (requestIds: string[]) => void
}

const FriendNotificationsContext = createContext<FriendNotificationsValue | null>(null)

const SEEN_KEY_PREFIX = 'nexa:seenFriendRequests:'

function readSeen(userId: string): string[] {
  try {
    const raw = localStorage.getItem(SEEN_KEY_PREFIX + userId)
    return raw ? (JSON.parse(raw) as string[]) : []
  } catch {
    return []
  }
}

function writeSeen(userId: string, ids: string[]): void {
  try {
    localStorage.setItem(SEEN_KEY_PREFIX + userId, JSON.stringify(ids))
  } catch {
    // A localStorage hiánya/megtelése nem kritikus — a badge csak kényelmi jelzés.
  }
}

export function FriendNotificationsProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const userId = user?.id ?? null

  // A szerverről kapott beérkezett kérés-azonosítók (egyszeri lekérés belépéskor).
  const [incomingIds, setIncomingIds] = useState<string[]>([])
  // A felhasználó által már látott kérés-azonosítók (a localStorage-ból inicializálva).
  const [seenIds, setSeenIds] = useState<string[]>([])

  useEffect(() => {
    if (!userId) {
      setIncomingIds([])
      setSeenIds([])
      return
    }
    setSeenIds(readSeen(userId))

    let active = true
    // Best-effort: a badge csak kényelmi jelzés, hibát itt nem mutatunk (a /friends oldal igen).
    getRequests()
      .then((r) => {
        if (active) setIncomingIds(r.incoming.map((req) => req.requestId))
      })
      .catch(() => {})
    return () => {
      active = false
    }
  }, [userId])

  // Kijelentkezéskor ürítjük az állapotot (lásd AuthContext azonos mintáját).
  useEffect(() => {
    const onLogout = () => {
      setIncomingIds([])
      setSeenIds([])
    }
    window.addEventListener(AUTH_LOGOUT_EVENT, onLogout)
    return () => window.removeEventListener(AUTH_LOGOUT_EVENT, onLogout)
  }, [])

  const markSeen = useCallback(
    (requestIds: string[]) => {
      if (!userId) return
      // A megadott lista a friss, aktuális beérkezett kérés-halmaz: a context incoming
      // állapotát is ehhez igazítjuk (különben egy elfogadott/elutasított kérés után a
      // belépéskor lekért, elavult lista alapján a badge visszajönne), és mindet látottá
      // tesszük → a badge 0. A látott halmazt felülírjuk, hogy ne hízzon elavult ID-kkel.
      setIncomingIds(requestIds)
      writeSeen(userId, requestIds)
      setSeenIds(requestIds)
    },
    [userId],
  )

  const unseenCount = useMemo(() => {
    const seen = new Set(seenIds)
    return incomingIds.filter((id) => !seen.has(id)).length
  }, [incomingIds, seenIds])

  const value = useMemo<FriendNotificationsValue>(
    () => ({ unseenCount, markSeen }),
    [unseenCount, markSeen],
  )

  return (
    <FriendNotificationsContext.Provider value={value}>
      {children}
    </FriendNotificationsContext.Provider>
  )
}

export function useFriendNotifications(): FriendNotificationsValue {
  const ctx = useContext(FriendNotificationsContext)
  if (!ctx) {
    throw new Error('useFriendNotifications csak FriendNotificationsProvider-en belül használható')
  }
  return ctx
}
