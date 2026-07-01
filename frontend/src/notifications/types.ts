// A backend perzisztált + valós idejű értesítése (lásd com.nexa.realtime.dto.NotificationDto, #17).

export type NotificationType =
  | 'NEW_POST'
  | 'FRIEND_REQUEST'
  | 'FRIEND_ACCEPTED'
  | 'NEW_FOLLOWER'
  | 'GROUP_JOIN_REQUEST'

export type NexaNotification = {
  /** Az értesítés azonosítója (a perzisztált entitás id-ja; olvasott-jelöléshez és kulcsként is). */
  id: string
  type: NotificationType
  /** Új bejegyzésnél a poszt azonosítója; kapcsolati típusoknál null. */
  postId: string | null
  /** Az aktor (a poszt szerzője / a kérelmező / a követő). */
  actorId: string
  actorName: string
  actorAvatarUrl: string | null
  /** Csoport-bejegyzésnél a forráscsoport; egyébként null. */
  groupId: string | null
  groupName: string | null
  groupLogoUrl: string | null
  /** ISO-8601 időbélyeg. */
  createdAt: string
  /** Olvasott-e már. */
  read: boolean
}
