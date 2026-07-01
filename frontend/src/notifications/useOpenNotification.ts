import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { requestFeedRefresh } from './feedRefresh'
import type { NexaNotification } from './types'

/**
 * Egy értesítésre kattintva a típusának megfelelő helyre navigál (#17):
 * <ul>
 *   <li>NEW_POST → a hírfolyamra, és kéri annak frissítését (az új poszt felül jelenik meg),</li>
 *   <li>FRIEND_REQUEST / FRIEND_ACCEPTED → az Ismerősök oldalra,</li>
 *   <li>NEW_FOLLOWER → az aktor profiljára,</li>
 *   <li>GROUP_JOIN_REQUEST → a csoport oldalára (a függő kérelmek ott jóváhagyhatók).</li>
 * </ul>
 */
export function useOpenNotification(): (n: NexaNotification) => void {
  const navigate = useNavigate()
  return useCallback(
    (n: NexaNotification) => {
      switch (n.type) {
        case 'FRIEND_REQUEST':
        case 'FRIEND_ACCEPTED':
          navigate('/friends')
          break
        case 'NEW_FOLLOWER':
          navigate(`/users/${n.actorId}`)
          break
        case 'GROUP_JOIN_REQUEST':
          if (n.groupId) {
            navigate(`/groups/${n.groupId}`)
          }
          break
        case 'NEW_POST':
        default:
          navigate('/')
          requestFeedRefresh()
          break
      }
    },
    [navigate],
  )
}
