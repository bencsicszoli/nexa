import { ApiError } from '../lib/api'

const KNOWN = new Set([
  'EMAIL_ALREADY_EXISTS',
  'INVALID_CREDENTIALS',
  'VALIDATION_ERROR',
  'UNAUTHENTICATED',
  'UNSUPPORTED_IMAGE_TYPE',
  'UNSUPPORTED_MEDIA_TYPE',
  'EMPTY_POST',
  'PAYLOAD_TOO_LARGE',
  'INVALID_UPLOAD',
  'USER_NOT_FOUND',
  'POST_NOT_FOUND',
  'SELF_FRIEND_REQUEST',
  'ALREADY_FRIENDS',
  'FRIEND_REQUEST_ALREADY_SENT',
  'REVERSE_FRIEND_REQUEST_EXISTS',
  'FRIEND_REQUEST_NOT_FOUND',
  'NOT_FRIENDS',
  'WRONG_PASSWORD',
  'INVALID_2FA_CODE',
  'INVALID_2FA_CHALLENGE',
  'TWO_FACTOR_ALREADY_ENABLED',
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
