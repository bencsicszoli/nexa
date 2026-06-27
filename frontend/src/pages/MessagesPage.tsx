import { useTranslation } from 'react-i18next'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, MessageCircle } from 'lucide-react'
import Avatar from '../components/Avatar'
import { formatRelativeTime } from '../lib/time'
import { useChat } from '../chat/ChatContext'
import ChatThread from '../chat/ChatThread'
import type { Conversation } from '../chat/types'

/**
 * A csevegés oldala (#12): bal oldalon a beszélgetéslista (élő előnézet, olvasatlan-jelvény,
 * online-pötty), jobb oldalon a kiválasztott szál. Az aktív szál id-ja az útvonalban van
 * (`/messages/:conversationId`), így a belépési pontok (profil „Üzenet", csoport „Csevegés")
 * közvetlenül a megfelelő szálra navigálhatnak.
 */
export default function MessagesPage() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { conversationId } = useParams<{ conversationId: string }>()
  const { conversations, onlineUserIds, typingByConversation } = useChat()

  const hasSelection = Boolean(conversationId)

  return (
    <div className="flex h-[calc(100vh-7rem)] overflow-hidden rounded-2xl border border-slate-200 bg-white">
      {/* Beszélgetéslista */}
      <aside
        className={`w-full shrink-0 flex-col border-r border-slate-200 sm:flex sm:w-72 lg:w-80 ${
          hasSelection ? 'hidden sm:flex' : 'flex'
        }`}
      >
        <header className="border-b border-slate-200 px-4 py-3">
          <h1 className="font-semibold text-slate-900">{t('chat.title')}</h1>
        </header>
        <div className="flex-1 overflow-y-auto">
          {conversations.length === 0 ? (
            <p className="px-4 py-8 text-center text-sm text-slate-400">{t('chat.empty')}</p>
          ) : (
            conversations.map((c) => (
              <ConversationRow
                key={c.id}
                conversation={c}
                active={c.id === conversationId}
                online={c.otherUserId ? onlineUserIds.has(c.otherUserId) : false}
                typing={Boolean(typingByConversation[c.id])}
                onClick={() => navigate(`/messages/${c.id}`)}
                lang={i18n.language}
              />
            ))
          )}
        </div>
      </aside>

      {/* Kiválasztott szál */}
      <section className={`min-w-0 flex-1 flex-col ${hasSelection ? 'flex' : 'hidden sm:flex'}`}>
        {conversationId ? (
          <>
            <button
              type="button"
              onClick={() => navigate('/messages')}
              className="flex items-center gap-2 border-b border-slate-200 px-4 py-2 text-sm text-slate-500 hover:bg-slate-50 sm:hidden"
            >
              <ArrowLeft className="h-4 w-4" />
              {t('chat.backToList')}
            </button>
            <div className="min-h-0 flex-1">
              <ChatThread conversationId={conversationId} />
            </div>
          </>
        ) : (
          <div className="flex flex-1 flex-col items-center justify-center gap-3 text-slate-400">
            <MessageCircle className="h-10 w-10" />
            <p className="text-sm">{t('chat.selectConversation')}</p>
          </div>
        )}
      </section>
    </div>
  )
}

function ConversationRow({
  conversation,
  active,
  online,
  typing,
  onClick,
  lang,
}: {
  conversation: Conversation
  active: boolean
  online: boolean
  typing: boolean
  onClick: () => void
  lang: string
}) {
  const { t } = useTranslation()
  const isGroup = conversation.type === 'GROUP'
  const preview = typing
    ? t('chat.typing')
    : conversation.lastMessagePreview ?? t('chat.noMessagesYet')

  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex w-full items-center gap-3 px-4 py-3 text-left transition-colors ${
        active ? 'bg-brand/10' : 'hover:bg-slate-50'
      }`}
    >
      <div className="relative shrink-0">
        <Avatar name={conversation.title} src={conversation.imageUrl} size="md" />
        {!isGroup && online && (
          <span className="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full border-2 border-white bg-emerald-500" />
        )}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex items-baseline justify-between gap-2">
          <span className="truncate text-sm font-semibold text-slate-900">{conversation.title}</span>
          <span className="shrink-0 text-[11px] text-slate-400">
            {formatRelativeTime(conversation.lastMessageAt, lang)}
          </span>
        </div>
        <div className="flex items-center justify-between gap-2">
          <span className={`truncate text-xs ${typing ? 'italic text-brand' : 'text-slate-500'}`}>
            {preview}
          </span>
          {conversation.unreadCount > 0 && (
            <span className="flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full bg-brand px-1.5 text-[11px] font-bold text-white">
              {conversation.unreadCount}
            </span>
          )}
        </div>
      </div>
    </button>
  )
}
