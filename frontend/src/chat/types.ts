// A backend /api/chat válaszainak és a STOMP-keretek típusai (lásd com.nexa.chat.*).

export type ConversationType = 'DIRECT' | 'GROUP'

/** Egy beszélgetés a listához és a szál fejlécéhez. */
export type Conversation = {
  id: string
  type: ConversationType
  /** A másik fél neve (DIRECT) vagy a csoport neve (GROUP). */
  title: string
  /** A másik fél avatárja (DIRECT) vagy a csoport logója (GROUP); lehet null. */
  imageUrl: string | null
  /** DIRECT szálnál a másik fél id-ja (profilhoz/jelenléthez); GROUP-nál null. */
  otherUserId: string | null
  /** GROUP szálnál a csoport id-ja (csoportoldalhoz); DIRECT-nél null. */
  groupId: string | null
  /** A másik fél online-e (csak DIRECT-nél értelmezett). */
  online: boolean
  /** A legutóbbi üzenet rövid előnézete (vagy null, ha még nincs üzenet). */
  lastMessagePreview: string | null
  /** ISO-8601 időbélyeg. */
  lastMessageAt: string
  unreadCount: number
}

/** Egy csevegő-üzenet. */
export type ChatMessage = {
  id: string
  conversationId: string
  senderId: string
  senderName: string
  senderAvatarUrl: string | null
  content: string
  createdAt: string
}

/** Egy lapnyi üzenet-előzmény (időrendben, legrégebbi elöl). */
export type MessagesPage = {
  conversation: Conversation
  messages: ChatMessage[]
  /** A régebbi üzenetek betöltéséhez; null, ha nincs több. */
  nextCursor: string | null
}

/** Gépelés-jelzés egy szálban (STOMP /user/queue/chat.typing). */
export type TypingNotice = {
  conversationId: string
  userId: string
  userName: string
}

/** Online/offline állapotváltás (STOMP /topic/presence). */
export type PresenceEvent = {
  userId: string
  online: boolean
}
