import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { requestFeedRefresh } from './feedRefresh'

/**
 * Értesítésre kattintáskor: a hírfolyamra navigál, és kéri annak frissítését, hogy az új
 * bejegyzés időrendben felül megjelenjen (#11 DoD). Ha már a hírfolyamon vagyunk, csak a
 * frissítés-esemény fut le.
 */
export function useOpenNotification(): () => void {
  const navigate = useNavigate()
  return useCallback(() => {
    navigate('/')
    requestFeedRefresh()
  }, [navigate])
}
