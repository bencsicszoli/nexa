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
