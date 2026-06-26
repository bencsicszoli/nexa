import type { TFunction } from 'i18next'
import type { NexaNotification } from './types'

/** Egy értesítés ember által olvasható szövege a kiválasztott nyelven. */
export function notificationText(n: NexaNotification, t: TFunction): string {
  if (n.groupId && n.groupName) {
    return t('notifications.newGroupPost', { name: n.actorName, group: n.groupName })
  }
  return t('notifications.newPost', { name: n.actorName })
}
