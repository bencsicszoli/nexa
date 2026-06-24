import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Loader2 } from 'lucide-react'
import { useAuth } from '../../auth/AuthContext'
import { errorKey } from '../../auth/errorKey'
import AuthLayout from './AuthLayout'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function RegisterPage() {
  const { t } = useTranslation()
  const { register } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<{ email?: string; displayName?: string; password?: string }>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function validate(): boolean {
    const next: typeof errors = {}
    if (!email.trim()) next.email = 'auth.validation.emailRequired'
    else if (!EMAIL_RE.test(email.trim())) next.email = 'auth.validation.emailInvalid'
    if (!displayName.trim()) next.displayName = 'auth.validation.displayNameRequired'
    else if (displayName.trim().length < 2) next.displayName = 'auth.validation.displayNameShort'
    if (!password) next.password = 'auth.validation.passwordRequired'
    else if (password.length < 8) next.password = 'auth.validation.passwordShort'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    if (!validate()) return
    setSubmitting(true)
    try {
      await register(email.trim(), displayName.trim(), password)
      navigate('/', { replace: true })
    } catch (err) {
      setFormError(errorKey(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout title={t('auth.registerTitle')} subtitle={t('auth.registerSubtitle')}>
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
          <span className="font-medium text-slate-700">{t('auth.displayName')}</span>
          <input
            type="text"
            autoComplete="name"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 outline-none transition-colors focus:border-brand focus:bg-white"
          />
          {errors.displayName && (
            <span className="text-xs text-rose-600">{t(errors.displayName)}</span>
          )}
        </label>

        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium text-slate-700">{t('auth.password')}</span>
          <input
            type="password"
            autoComplete="new-password"
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
          {submitting ? t('auth.signingUp') : t('auth.signUp')}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-500">
        {t('auth.haveAccount')}{' '}
        <Link to="/login" className="font-semibold text-brand hover:underline">
          {t('auth.toLogin')}
        </Link>
      </p>
    </AuthLayout>
  )
}
