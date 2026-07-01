// A backend /api/auth válaszainak típusai (lásd com.nexa.auth.dto).

export type User = {
  id: string
  email: string
  displayName: string
  bio: string | null
  avatarUrl: string | null
  coverUrl: string | null
  role: string
  /** A felület nyelve (#17) — belépéskor ez az igazság forrása. */
  locale?: string
  /** Be van-e kapcsolva a kétlépcsős hitelesítés (#17). */
  totpEnabled?: boolean
  createdAt: string
}

export type AuthResponse = {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: User
}

/**
 * A bejelentkezés kimenete (#17): vagy bejelentkezett felhasználó, vagy — ha be van kapcsolva a
 * 2FA — egy challenge token, amivel a kód megadása után a {@code loginWith2faRequest} fejezi be.
 */
export type LoginResult =
  | { kind: 'authenticated'; user: User }
  | { kind: '2fa'; challengeToken: string }

// A backend egységes hibaválasza: { code, message, fields? }.
export type ApiErrorBody = {
  code: string
  message?: string
  fields?: Record<string, string>
}
