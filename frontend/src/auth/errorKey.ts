import { ApiError } from '../lib/api'

const KNOWN = new Set([
  'EMAIL_ALREADY_EXISTS',
  'INVALID_CREDENTIALS',
  'VALIDATION_ERROR',
  'UNAUTHENTICATED',
])

/**
 * Egy elkapott hibából a megfelelő i18n kulcsot adja vissza (auth.error.*).
 * Ismeretlen backend-kód → UNKNOWN_ERROR; hálózati hiba (nem ApiError) → NETWORK.
 */
export function errorKey(err: unknown): string {
  if (err instanceof ApiError) {
    return `auth.error.${KNOWN.has(err.code) ? err.code : 'UNKNOWN_ERROR'}`
  }
  return 'auth.error.NETWORK'
}
