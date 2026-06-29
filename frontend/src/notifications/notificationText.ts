import type { TFunction } from 'i18next'
import type { NexaNotification } from './types'

/** Egy értesítés ember által olvasható szövege a kiválasztott nyelven, típus szerint (#17). */
export function notificationText(n: NexaNotification, t: TFunction): string {
  switch (n.type) {
    case 'FRIEND_REQUEST':
      return t('notifications.friendRequest', { name: n.actorName })
    case 'FRIEND_ACCEPTED':
      return t('notifications.friendAccepted', { name: n.actorName })
    case 'NEW_FOLLOWER':
      return t('notifications.newFollower', { name: n.actorName })
    case 'NEW_POST':
    default:
      if (n.groupId && n.groupName) {
        return t('notifications.newGroupPost', { name: n.actorName, group: n.groupName })
      }
      return t('notifications.newPost', { name: n.actorName })
  }
}
