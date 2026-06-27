// Token-tárolás. Az access + refresh tokent localStorage-ban tartjuk, hogy az
// oldalújratöltés után is bejelentkezve maradjon a felhasználó.
//
// Megjegyzés (hardening, #18): a refresh token httpOnly cookie-ban biztonságosabb
// lenne (XSS-ellenállóbb). Ezt a kártyát egyszerűsített, body-alapú tokenfolyammal
// zárjuk; a cookie-ra váltás a biztonsági megerősítő kártya feladata.

const ACCESS_KEY = 'nexa.accessToken'
const REFRESH_KEY = 'nexa.refreshToken'

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

export function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_KEY, accessToken)
  localStorage.setItem(REFRESH_KEY, refreshToken)
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

/** Az access JWT lejárati ideje epoch-ms-ben, vagy null, ha nem dekódolható. */
function accessTokenExpiry(): number | null {
  const token = getAccessToken()
  if (!token) return null
  try {
    const payload = token.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    const exp = JSON.parse(json).exp
    return typeof exp === 'number' ? exp * 1000 : null
  } catch {
    return null
  }
}

/**
 * Igaz, ha nincs access token, vagy a meglévő a megadott időablakon belül lejár (vagy már lejárt).
 * A STOMP-kapcsolat ezzel dönti el, hogy (újra)csatlakozás előtt frissítse-e a tokent. Ha a lejárat
 * nem dekódolható (null), nem erőltetünk frissítést (false) — a normál apiFetch-folyam úgyis kezeli.
 */
export function accessTokenExpiringWithin(ms: number): boolean {
  const token = getAccessToken()
  if (!token) return true
  const exp = accessTokenExpiry()
  if (exp === null) return false
  return Date.now() >= exp - ms
}
