// A backend valós idejű értesítése (lásd com.nexa.realtime.dto.NotificationDto).
// Egyelőre egyféle típus: új bejegyzés egy kapcsolattól.

export type NotificationType = 'NEW_POST'

export type NexaNotification = {
  /** Az értesítés egyedi azonosítója (a backend generálja; React-kulcsként is használjuk). */
  id: string
  type: NotificationType
  /** Az új bejegyzés azonosítója. */
  postId: string
  /** A bejegyzés szerzője. */
  actorId: string
  actorName: string
  actorAvatarUrl: string | null
  /** Csoport-bejegyzésnél a forráscsoport; profil-posztnál null. */
  groupId: string | null
  groupName: string | null
  groupLogoUrl: string | null
  /** ISO-8601 időbélyeg. */
  createdAt: string
}
