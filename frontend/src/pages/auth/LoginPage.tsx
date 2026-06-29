import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Loader2 } from 'lucide-react'
import { useAuth } from '../../auth/AuthContext'
import { errorKey } from '../../auth/errorKey'
import AuthLayout from './AuthLayout'

type LocationState = { from?: { pathname: string } }

export default function LoginPage() {
  const { t } = useTranslation()
  const { login, loginWith2fa } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const redirectTo = (location.state as LocationState | null)?.from?.pathname ?? '/'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // Kétlépcsős login: ha a jelszó után 2FA kell, ide kerül a challenge token, és a kódbekérő mező jelenik meg.
  const [challengeToken, setChallengeToken] = useState<string | null>(null)
  const [code, setCode] = useState('')
  const [useRecovery, setUseRecovery] = useState(false)

  function validate(): boolean {
    const next: typeof errors = {}
    if (!email.trim()) next.email = 'auth.validation.emailRequired'
    if (!password) next.password = 'auth.validation.passwordRequired'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    if (!validate()) return
    setSubmitting(true)
    try {
      const result = await login(email.trim(), password)
      if (result.kind === '2fa') {
        // Második lépés: a felhasználónak 2FA kódot kell megadnia.
        setChallengeToken(result.challengeToken)
      } else {
        navigate(redirectTo, { replace: true })
      }
    } catch (err) {
      setFormError(errorKey(err))
    } finally {
      setSubmitting(false)
    }
  }

  async function onSubmit2fa(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    if (!code.trim() || !challengeToken) return
    setSubmitting(true)
    try {
      await loginWith2fa(challengeToken, code.trim())
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setFormError(errorKey(err))
    } finally {
      setSubmitting(false)
    }
  }

  // --- Második lépés: 2FA kód ---
  if (challengeToken) {
    return (
      <AuthLayout title={t('auth.twoFactorTitle')} subtitle={t('auth.twoFactorSubtitle')}>
        <form onSubmit={onSubmit2fa} noValidate className="flex flex-col gap-4">
          {formError && (
            <div role="alert" className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">
              {t(formError)}
            </div>
          )}

          <label className="flex flex-col gap-1 text-sm">
            <span className="font-medium text-slate-700">
              {useRecovery ? t('auth.recoveryCode') : t('auth.twoFactorCode')}
            </span>
            <input
              type="text"
              inputMode={useRecovery ? 'text' : 'numeric'}
              autoComplete="one-time-code"
              autoFocus
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder={useRecovery ? 'XXXX-XXXX' : '000000'}
              className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 tracking-widest outline-none transition-colors focus:border-brand focus:bg-white"
            />
          </label>

          <button
            type="submit"
            disabled={submitting}
            className="mt-2 flex items-center justify-center gap-2 rounded-lg bg-brand px-4 py-2.5 font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
          >
            {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
            {submitting ? t('auth.verifying') : t('auth.verify')}
          </button>

          <button
            type="button"
            onClick={() => {
              setUseRecovery((v) => !v)
              setCode('')
              setFormError(null)
            }}
            className="text-center text-sm font-medium text-brand hover:underline"
          >
            {useRecovery ? t('auth.useAuthenticator') : t('auth.useRecoveryCode')}
          </button>
        </form>
      </AuthLayout>
    )
  }

  // --- Első lépés: e-mail + jelszó ---
  return (
    <AuthLayout title={t('auth.loginTitle')} subtitle={t('auth.loginSubtitle')}>
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
        {formError && (
          <div role="alert" className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {t(formError)}
          </div>
        )}

        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium text-slate-700">{t('auth.email')}</span>
          <input
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 outline-none transition-colors focus:border-brand focus:bg-white"
          />
          {errors.email && <span className="text-xs text-rose-600">{t(errors.email)}</span>}
        </label>

        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium text-slate-700">{t('auth.password')}</span>
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 outline-none transition-colors focus:border-brand focus:bg-white"
          />
          {errors.password && <span className="text-xs text-rose-600">{t(errors.password)}</span>}
        </label>

        <button
          type="submit"
          disabled={submitting}
          className="mt-2 flex items-center justify-center gap-2 rounded-lg bg-brand px-4 py-2.5 font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
        >
          {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
          {submitting ? t('auth.signingIn') : t('auth.signIn')}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-500">
        {t('auth.noAccount')}{' '}
        <Link to="/register" className="font-semibold text-brand hover:underline">
          {t('auth.toRegister')}
        </Link>
      </p>
    </AuthLayout>
  )
}
