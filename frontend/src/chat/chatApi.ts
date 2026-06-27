import { apiFetch } from '../lib/api'
import type { Conversation, MessagesPage } from './types'

// A backend /api/chat végpontjai (lásd com.nexa.chat.ChatController).

/** A bejelentkezett felhasználó beszélgetései, legutóbb aktív felül. */
export function getConversations(): Promise<Conversation[]> {
  return apiFetch<Conversation[]>('/chat/conversations')
}

/** Kétszemélyes szál megnyitása/létrehozása egy másik felhasználóval. */
export function startDirect(userId: string): Promise<Conversation> {
  return apiFetch<Conversation>('/chat/conversations/direct', {
    method: 'POST',
    body: { userId },
  })
}

/** Egy csoport csevegő-szálának megnyitása/létrehozása (csak tagnak). */
export function openGroupConversation(groupId: string): Promise<Conversation> {
  return apiFetch<Conversation>(`/chat/conversations/group/${groupId}`, { method: 'POST' })
}

/** Egy szál üzenet-előzménye (időrendben) + friss metaadat. Olvasottá teszi a szálat. */
export function getMessages(conversationId: string, cursor?: string): Promise<MessagesPage> {
  const suffix = cursor ? `?cursor=${encodeURIComponent(cursor)}` : ''
  return apiFetch<MessagesPage>(`/chat/conversations/${conversationId}/messages${suffix}`)
}

/** A szál olvasottá jelölése (az olvasatlan-jelvény nullázásához). */
export function markConversationRead(conversationId: string): Promise<void> {
  return apiFetch<void>(`/chat/conversations/${conversationId}/read`, { method: 'POST' })
}
