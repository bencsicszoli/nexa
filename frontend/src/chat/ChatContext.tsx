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
import { useAuth } from '../auth/AuthContext'
import { useStomp } from '../realtime/StompProvider'
import {
  getConversations,
  markConversationRead,
  openGroupConversation,
  startDirect,
} from './chatApi'
import type { ChatMessage, Conversation, PresenceEvent, TypingNotice } from './types'

/**
 * A csevegés (#12) kliensoldali állapota a megosztott STOMP-kapcsolaton ({@link useStomp}).
 * Nyilvántartja a beszélgetéslistát (élő előnézettel és olvasatlan-számmal), a jelenlétet és a
 * gépelés-jelzést; a beérkező üzeneteket egyúttal a nyitott szálnak is továbbítja
 * ({@link ChatContextValue.subscribeToMessages}).
 *
 * Az üzenetküldés és a gépelés-jelzés STOMP-on megy (`/app/chat.send`, `/app/chat.typing`); az
 * előzmény és az olvasottság REST-en (lásd {@link chatApi}). A szerver minden üzenetet a küldőnek
 * is visszaküld, így a saját buborék is a szerver visszaigazolásából jelenik meg (nincs optimista
 * duplikáció).
 */
type TypingState = { userId: string; userName: string }

type ChatContextValue = {
  conversations: Conversation[]
  totalUnread: number
  onlineUserIds: Set<string>
  typingByConversation: Record<string, TypingState | undefined>
  refresh: () => Promise<void>
  openDirect: (userId: string) => Promise<Conversation>
  openGroup: (groupId: string) => Promise<Conversation>
  sendMessage: (conversationId: string, content: string) => void
  sendTyping: (conversationId: string) => void
  markRead: (conversationId: string) => void
  setActiveConversation: (conversationId: string | null) => void
  subscribeToMessages: (handler: (message: ChatMessage) => void) => () => void
}

const ChatContext = createContext<ChatContextValue | null>(null)

/** Ennyi ideig mutatjuk a „gépel…" jelzést új keret nélkül (ms). */
const TYPING_TIMEOUT_MS = 4000

function sortByRecent(list: Conversation[]): Conversation[] {
  // Az ISO-8601 UTC időbélyegek lexikografikusan = időrendben rendezhetők.
  return [...list].sort((a, b) => (a.lastMessageAt < b.lastMessageAt ? 1 : -1))
}

export function ChatProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const meId = user?.id ?? null
  const { subscribe, publish } = useStomp()

  const [conversations, setConversations] = useState<Conversation[]>([])
  const [onlineUserIds, setOnlineUserIds] = useState<Set<string>>(new Set())
  const [typingByConversation, setTyping] = useState<Record<string, TypingState | undefined>>({})

  // Refs, hogy a STOMP-handlerek a friss értékeket lássák újra-feliratkozás nélkül.
  const activeIdRef = useRef<string | null>(null)
  const meIdRef = useRef<string | null>(meId)
  meIdRef.current = meId
  const typingTimers = useRef<Map<string, number>>(new Map())
  const messageSubscribers = useRef<Set<(m: ChatMessage) => void>>(new Set())

  const refresh = useCallback(async () => {
    const list = await getConversations()
    setConversations(sortByRecent(list))
    // Az online-pöttyökhöz a lista pillanatképéből indulunk; a továbbiakban a /topic/presence frissít.
    setOnlineUserIds((prev) => {
      const next = new Set(prev)
      for (const c of list) {
        if (c.otherUserId) {
          if (c.online) next.add(c.otherUserId)
          else next.delete(c.otherUserId)
        }
      }
      return next
    })
  }, [])

  /** Beszúr/frissít egy beszélgetést a listában, és előre rendezi. */
  const upsertConversation = useCallback((conversation: Conversation) => {
    setConversations((prev) => {
      const without = prev.filter((c) => c.id !== conversation.id)
      return sortByRecent([conversation, ...without])
    })
    // A frissen megnyitott szál szerver által számolt online állapotát is átvesszük, hogy a
    // zöld pötty az ELSŐ üzenet előtt is helyes legyen. A /topic/presence csak átmenetkor
    // (be-/kijelentkezés) szól, így enélkül egy korábban már online partner offline-nak látszana.
    if (conversation.otherUserId) {
      const otherId = conversation.otherUserId
      setOnlineUserIds((prev) => {
        const next = new Set(prev)
        if (conversation.online) next.add(otherId)
        else next.delete(otherId)
        return next
      })
    }
  }, [])

  const markRead = useCallback((conversationId: string) => {
    setConversations((prev) =>
      prev.map((c) => (c.id === conversationId ? { ...c, unreadCount: 0 } : c)),
    )
    void markConversationRead(conversationId).catch(() => {})
  }, [])

  const setActiveConversation = useCallback(
    (conversationId: string | null) => {
      activeIdRef.current = conversationId
      if (conversationId) markRead(conversationId)
    },
    [markRead],
  )

  const openDirect = useCallback(
    async (userId: string) => {
      const conversation = await startDirect(userId)
      upsertConversation(conversation)
      return conversation
    },
    [upsertConversation],
  )

  const openGroup = useCallback(
    async (groupId: string) => {
      const conversation = await openGroupConversation(groupId)
      upsertConversation(conversation)
      return conversation
    },
    [upsertConversation],
  )

  const sendMessage = useCallback(
    (conversationId: string, content: string) => {
      publish('/app/chat.send', { conversationId, content })
    },
    [publish],
  )

  const sendTyping = useCallback(
    (conversationId: string) => {
      publish('/app/chat.typing', { conversationId })
    },
    [publish],
  )

  const subscribeToMessages = useCallback((handler: (m: ChatMessage) => void) => {
    messageSubscribers.current.add(handler)
    return () => {
      messageSubscribers.current.delete(handler)
    }
  }, [])

  // Beérkező üzenet kezelése: a lista frissítése + továbbítás a nyitott szálnak.
  const handleIncoming = useCallback(
    (message: ChatMessage) => {
      messageSubscribers.current.forEach((handler) => handler(message))

      const isActive = activeIdRef.current === message.conversationId
      const fromMe = message.senderId === meIdRef.current

      setConversations((prev) => {
        const existing = prev.find((c) => c.id === message.conversationId)
        if (!existing) {
          // Ismeretlen szál (pl. valaki most írt rád először) — töltsük újra a listát.
          void refresh()
          return prev
        }
        const updated: Conversation = {
          ...existing,
          lastMessagePreview: message.content,
          lastMessageAt: message.createdAt,
          unreadCount:
            isActive || fromMe ? existing.unreadCount : existing.unreadCount + 1,
        }
        return sortByRecent([updated, ...prev.filter((c) => c.id !== message.conversationId)])
      })

      // A nyitott szálban azonnal olvasottá tesszük a beérkező üzenetet a szerveren is.
      if (isActive && !fromMe) {
        void markConversationRead(message.conversationId).catch(() => {})
      }
    },
    [refresh],
  )

  // Feliratkozások a megosztott kapcsolaton + a lista első betöltése.
  useEffect(() => {
    if (!meId) return
    void refresh()

    const unsubMessages = subscribe('/user/queue/messages', (frame) => {
      try {
        handleIncoming(JSON.parse(frame.body) as ChatMessage)
      } catch {
        /* hibás keret — kihagyjuk */
      }
    })

    const unsubTyping = subscribe('/user/queue/chat.typing', (frame) => {
      try {
        const notice = JSON.parse(frame.body) as TypingNotice
        setTyping((prev) => ({
          ...prev,
          [notice.conversationId]: { userId: notice.userId, userName: notice.userName },
        }))
        const timers = typingTimers.current
        const existing = timers.get(notice.conversationId)
        if (existing) window.clearTimeout(existing)
        const timer = window.setTimeout(() => {
          setTyping((prev) => ({ ...prev, [notice.conversationId]: undefined }))
          timers.delete(notice.conversationId)
        }, TYPING_TIMEOUT_MS)
        timers.set(notice.conversationId, timer)
      } catch {
        /* hibás keret — kihagyjuk */
      }
    })

    const unsubPresence = subscribe('/topic/presence', (frame) => {
      try {
        const event = JSON.parse(frame.body) as PresenceEvent
        setOnlineUserIds((prev) => {
          const next = new Set(prev)
          if (event.online) next.add(event.userId)
          else next.delete(event.userId)
          return next
        })
      } catch {
        /* hibás keret — kihagyjuk */
      }
    })

    return () => {
      unsubMessages()
      unsubTyping()
      unsubPresence()
      typingTimers.current.forEach((timer) => window.clearTimeout(timer))
      typingTimers.current.clear()
    }
  }, [meId, subscribe, refresh, handleIncoming])

  const totalUnread = useMemo(
    () => conversations.reduce((sum, c) => sum + c.unreadCount, 0),
    [conversations],
  )

  const value = useMemo<ChatContextValue>(
    () => ({
      conversations,
      totalUnread,
      onlineUserIds,
      typingByConversation,
      refresh,
      openDirect,
      openGroup,
      sendMessage,
      sendTyping,
      markRead,
      setActiveConversation,
      subscribeToMessages,
    }),
    [
      conversations,
      totalUnread,
      onlineUserIds,
      typingByConversation,
      refresh,
      openDirect,
      openGroup,
      sendMessage,
      sendTyping,
      markRead,
      setActiveConversation,
      subscribeToMessages,
    ],
  )

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>
}

export function useChat(): ChatContextValue {
  const ctx = useContext(ChatContext)
  if (!ctx) {
    throw new Error('useChat csak ChatProvider-en belül használható')
  }
  return ctx
}
