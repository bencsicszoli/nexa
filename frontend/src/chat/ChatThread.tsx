import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Loader2, Send } from 'lucide-react'
import Avatar from '../components/Avatar'
import { useAuth } from '../auth/AuthContext'
import { errorKey } from '../auth/errorKey'
import { formatRelativeTime } from '../lib/time'
import { useChat } from './ChatContext'
import { getMessages } from './chatApi'
import type { ChatMessage, Conversation } from './types'

/** Két gépelés-jelzés közti minimális szünet (ms), hogy ne küldjünk minden leütésre. */
const TYPING_THROTTLE_MS = 2500

/**
 * Egy megnyitott beszélgetés szála (#12): előzmény-betöltés, élő üzenetek, gépelés-jelző és a
 * küldődoboz. Az élő üzeneteket a {@link useChat} továbbítja; a küldés STOMP-on megy, a saját
 * buborék a szerver visszaigazolásából jelenik meg.
 */
export default function ChatThread({ conversationId }: { conversationId: string }) {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  const meId = user?.id ?? null
  const {
    conversations,
    onlineUserIds,
    typingByConversation,
    sendMessage,
    sendTyping,
    setActiveConversation,
    subscribeToMessages,
  } = useChat()

  const [header, setHeader] = useState<Conversation | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [cursor, setCursor] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadingOlder, setLoadingOlder] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [draft, setDraft] = useState('')

  const bottomRef = useRef<HTMLDivElement | null>(null)
  const lastTypingSentRef = useRef(0)
  const textareaRef = useRef<HTMLTextAreaElement | null>(null)

  // A küldődoboz a tartalomhoz nő (a CSS max-h-32-ig, utána görget); küldés után
  // a kiürült draft visszaállítja egysorosra.
  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${el.scrollHeight}px`
  }, [draft])

  // A listából frissített fejléc (online-pötty, cím) — a REST-fejléc a tartalék.
  const fromList = conversations.find((c) => c.id === conversationId)
  const view = fromList ?? header

  // Az aktív szál beállítása (olvasottság + a beérkező üzenet ne növelje az olvasatlant).
  useEffect(() => {
    setActiveConversation(conversationId)
    return () => setActiveConversation(null)
  }, [conversationId, setActiveConversation])

  // Előzmény betöltése a szál váltásakor.
  useEffect(() => {
    let active = true
    setLoading(true)
    setError(null)
    setMessages([])
    getMessages(conversationId)
      .then((page) => {
        if (!active) return
        setHeader(page.conversation)
        setMessages(page.messages)
        setCursor(page.nextCursor)
      })
      .catch((err) => active && setError(errorKey(err)))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [conversationId])

  // Élő üzenetek fogadása a nyitott szálba (duplikátumvédelemmel).
  useEffect(() => {
    return subscribeToMessages((message) => {
      if (message.conversationId !== conversationId) return
      setMessages((prev) => (prev.some((m) => m.id === message.id) ? prev : [...prev, message]))
    })
  }, [conversationId, subscribeToMessages])

  // Görgetés az aljára új üzenetnél / szálváltáskor.
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: 'end' })
  }, [messages.length, loading])

  const loadOlder = async () => {
    if (!cursor || loadingOlder) return
    setLoadingOlder(true)
    try {
      const page = await getMessages(conversationId, cursor)
      // A régebbi lap időrendben (régebbi elöl) jön — a meglévők elé fűzzük.
      setMessages((prev) => {
        const ids = new Set(prev.map((m) => m.id))
        const older = page.messages.filter((m) => !ids.has(m.id))
        return [...older, ...prev]
      })
      setCursor(page.nextCursor)
    } catch {
      /* a régebbi lap betöltése best-effort */
    } finally {
      setLoadingOlder(false)
    }
  }

  const onDraftChange = (value: string) => {
    setDraft(value)
    const now = Date.now()
    if (now - lastTypingSentRef.current > TYPING_THROTTLE_MS) {
      lastTypingSentRef.current = now
      sendTyping(conversationId)
    }
  }

  const sendNow = () => {
    const text = draft.trim()
    if (!text) return
    sendMessage(conversationId, text)
    setDraft('')
    lastTypingSentRef.current = 0
  }

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    sendNow()
  }

  // Enter küld; Shift+Enter új sort kezd. (IME-kompozíció közben — pl. ékezetes
  // bevitel — ne küldjünk, hagyjuk a böngészőre az Entert.)
  const onKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault()
      sendNow()
    }
  }

  const typing = typingByConversation[conversationId]
  const showTyping = typing && typing.userId !== meId
  const isGroup = view?.type === 'GROUP'
  const online = view?.otherUserId ? onlineUserIds.has(view.otherUserId) : false

  return (
    <div className="flex h-full flex-col">
      {/* Fejléc */}
      <header className="flex items-center gap-3 border-b border-slate-200 px-4 py-3">
        {view && (
          <>
            <div className="relative">
              <Avatar name={view.title} src={view.imageUrl} size="md" />
              {!isGroup && online && (
                <span
                  className="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full border-2 border-white bg-emerald-500"
                  aria-label={t('chat.online')}
                />
              )}
            </div>
            <div className="min-w-0">
              <p className="truncate font-semibold text-slate-900">{view.title}</p>
              {!isGroup && (
                <p className="text-xs text-slate-400">
                  {online ? t('chat.online') : t('chat.offline')}
                </p>
              )}
              {isGroup && <p className="text-xs text-slate-400">{t('chat.groupChat')}</p>}
            </div>
          </>
        )}
      </header>

      {/* Üzenetek */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        {loading ? (
          <div className="flex justify-center py-10 text-slate-400">
            <Loader2 className="h-5 w-5 animate-spin" />
          </div>
        ) : error ? (
          <p className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600" role="alert">
            {t(error)}
          </p>
        ) : messages.length === 0 ? (
          <p className="py-10 text-center text-sm text-slate-400">{t('chat.noMessages')}</p>
        ) : (
          <div className="flex flex-col gap-2">
            {cursor && (
              <button
                type="button"
                onClick={loadOlder}
                disabled={loadingOlder}
                className="mx-auto mb-2 rounded-full border border-slate-200 px-3 py-1 text-xs font-medium text-slate-500 hover:bg-slate-100 disabled:opacity-60"
              >
                {loadingOlder ? t('chat.loadingOlder') : t('chat.loadOlder')}
              </button>
            )}
            {messages.map((message, index) => {
              const mine = message.senderId === meId
              // Csoportban a feladó nevét csak akkor írjuk ki, ha vált a feladó.
              const showSender =
                isGroup && !mine && messages[index - 1]?.senderId !== message.senderId
              return (
                <div key={message.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                  <div className="max-w-[75%]">
                    {showSender && (
                      <p className="mb-0.5 ml-1 text-xs font-medium text-slate-500">
                        {message.senderName}
                      </p>
                    )}
                    <div
                      className={`whitespace-pre-wrap break-words rounded-2xl px-3 py-2 text-sm ${
                        mine
                          ? 'rounded-br-sm bg-brand text-white'
                          : 'rounded-bl-sm bg-slate-100 text-slate-800'
                      }`}
                    >
                      {message.content}
                    </div>
                    <p className={`mt-0.5 text-[11px] text-slate-400 ${mine ? 'text-right' : 'text-left'}`}>
                      {formatRelativeTime(message.createdAt, i18n.language)}
                    </p>
                  </div>
                </div>
              )
            })}
            <div ref={bottomRef} />
          </div>
        )}
      </div>

      {/* Gépelés-jelző */}
      {showTyping && (
        <div className="px-4 pb-1 text-xs italic text-slate-400">
          {isGroup ? t('chat.typingNamed', { name: typing!.userName }) : t('chat.typing')}
        </div>
      )}

      {/* Küldődoboz */}
      <form onSubmit={submit} className="flex items-end gap-2 border-t border-slate-200 p-3">
        <textarea
          ref={textareaRef}
          value={draft}
          onChange={(e) => onDraftChange(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder={t('chat.messagePlaceholder')}
          rows={1}
          className="max-h-32 min-h-[40px] min-w-0 flex-1 resize-none rounded-2xl border border-slate-200 bg-slate-50 px-4 py-2 text-sm outline-none transition-colors placeholder:text-slate-400 focus:border-brand focus:bg-white"
          maxLength={4000}
        />
        <button
          type="submit"
          disabled={!draft.trim()}
          className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-brand text-white transition-colors hover:bg-brand-dark disabled:opacity-50"
          aria-label={t('chat.send')}
        >
          <Send className="h-4 w-4" />
        </button>
      </form>
    </div>
  )
}
