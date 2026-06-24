// A backend /api/auth válaszainak típusai (lásd com.nexa.auth.dto).

export type User = {
  id: string
  email: string
  displayName: string
  role: string
  createdAt: string
}

export type AuthResponse = {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: User
}

// A backend egységes hibaválasza: { code, message, fields? }.
export type ApiErrorBody = {
  code: string
  message?: string
  fields?: Record<string, string>
}
