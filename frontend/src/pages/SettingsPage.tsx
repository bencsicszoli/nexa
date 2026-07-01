import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import QRCode from 'qrcode'
import { Loader2, ShieldCheck, ShieldOff } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { errorKey } from '../auth/errorKey'
import {
  begin2faSetup,
  changePassword,
  disable2fa,
  enable2fa,
  getSettings,
  updateLocale,
  updateNotificationPrefs,
  updatePrivacy,
  type NotificationPrefs,
  type Settings,
} from '../settings/settingsApi'

type Feedback = { kind: 'ok' | 'error'; key: string } | null

/** Egyszerű kapcsoló (toggle) a beállításokhoz. */
function Toggle({
  checked,
  onChange,
  label,
  disabled,
}: {
  checked: boolean
  onChange: (next: boolean) => void
  label: string
  disabled?: boolean
}) {
  return (
    <label className="flex cursor-pointer items-center justify-between gap-4 py-2">
      <span className="text-sm text-slate-700">{label}</span>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-label={label}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={`relative h-6 w-11 shrink-0 rounded-full transition-colors disabled:opacity-50 ${
          checked ? 'bg-brand' : 'bg-slate-300'
        }`}
      >
        <span
          className={`absolute top-0.5 h-5 w-5 rounded-full bg-white transition-all ${
            checked ? 'left-[22px]' : 'left-0.5'
          }`}
        />
      </button>
    </label>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-6">
      <h2 className="mb-4 text-base font-semibold text-slate-900">{title}</h2>
      {children}
    </section>
  )
}

export default function SettingsPage() {
  const { t, i18n } = useTranslation()
  const { logout } = useAuth()
  const navigate = useNavigate()

  const [settings, setSettings] = useState<Settings | null>(null)
  const [loadError, setLoadError] = useState(false)

  useEffect(() => {
    getSettings()
      .then(setSettings)
      .catch(() => setLoadError(true))
  }, [])

  if (loadError) {
    return (
      <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-6 text-center text-sm text-rose-600" role="alert">
        {t('settings.loadError')}
      </div>
    )
  }
  if (!settings) {
    return (
      <div className="flex justify-center py-10 text-slate-400">
        <Loader2 className="h-6 w-6 animate-spin" />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <header className="rounded-2xl border border-slate-200 bg-white p-6">
        <h1 className="text-lg font-semibold text-slate-900">{t('settings.title')}</h1>
        <p className="mt-0.5 text-sm text-slate-500">{t('settings.subtitle')}</p>
      </header>

      <LanguageSection
        settings={settings}
        onChange={(s) => setSettings(s)}
        changeLanguage={(l) => {
          void i18n.changeLanguage(l)
          localStorage.setItem('nexa.lang', l)
        }}
      />
      <NotificationsSection settings={settings} onChange={setSettings} />
      <PrivacySection settings={settings} onChange={setSettings} />
      <PasswordSection
        onChanged={async () => {
          // A jelszóváltás minden refresh tokent visszavon → kontrollált kiléptetés.
          await logout()
          navigate('/login', { replace: true })
        }}
      />
      <TwoFactorSection settings={settings} onChange={setSettings} />
    </div>
  )
}

// --- Nyelv ---
function LanguageSection({
  settings,
  onChange,
  changeLanguage,
}: {
  settings: Settings
  onChange: (s: Settings) => void
  changeLanguage: (locale: string) => void
}) {
  const { t } = useTranslation()
  const [saving, setSaving] = useState(false)

  async function select(locale: string) {
    if (locale === settings.locale) return
    setSaving(true)
    try {
      const next = await updateLocale(locale)
      onChange(next)
      changeLanguage(locale)
    } catch {
      // némán; a UI nyelve nem vált, ha a mentés elbukik
    } finally {
      setSaving(false)
    }
  }

  return (
    <Section title={t('settings.language')}>
      <div className="flex gap-2">
        {(['hu', 'en'] as const).map((l) => (
          <button
            key={l}
            type="button"
            disabled={saving}
            onClick={() => select(l)}
            className={`rounded-lg border px-4 py-2 text-sm font-medium transition-colors disabled:opacity-60 ${
              settings.locale === l
                ? 'border-brand bg-brand/10 text-brand'
                : 'border-slate-200 text-slate-600 hover:bg-slate-100'
            }`}
          >
            {t(`lang.${l}`)}
          </button>
        ))}
      </div>
    </Section>
  )
}

// --- Értesítési preferenciák ---
function NotificationsSection({
  settings,
  onChange,
}: {
  settings: Settings
  onChange: (s: Settings) => void
}) {
  const { t } = useTranslation()
  const [saving, setSaving] = useState(false)
  const prefs = settings.notificationPrefs

  async function toggle(key: keyof NotificationPrefs, value: boolean) {
    setSaving(true)
    try {
      const next = await updateNotificationPrefs({ ...prefs, [key]: value })
      onChange(next)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Section title={t('settings.notifications')}>
      <div className="divide-y divide-slate-100">
        <Toggle label={t('settings.notifNewPost')} checked={prefs.newPost} disabled={saving} onChange={(v) => toggle('newPost', v)} />
        <Toggle label={t('settings.notifFriendRequest')} checked={prefs.friendRequest} disabled={saving} onChange={(v) => toggle('friendRequest', v)} />
        <Toggle label={t('settings.notifFriendAccepted')} checked={prefs.friendAccepted} disabled={saving} onChange={(v) => toggle('friendAccepted', v)} />
        <Toggle label={t('settings.notifNewFollower')} checked={prefs.newFollower} disabled={saving} onChange={(v) => toggle('newFollower', v)} />
        <Toggle label={t('settings.notifGroupJoinRequest')} checked={prefs.groupJoinRequest} disabled={saving} onChange={(v) => toggle('groupJoinRequest', v)} />
      </div>
    </Section>
  )
}

// --- Adatvédelem ---
function PrivacySection({
  settings,
  onChange,
}: {
  settings: Settings
  onChange: (s: Settings) => void
}) {
  const { t } = useTranslation()
  const [saving, setSaving] = useState(false)

  async function save(searchable: boolean, hidePresence: boolean) {
    setSaving(true)
    try {
      const next = await updatePrivacy(searchable, hidePresence)
      onChange(next)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Section title={t('settings.privacy')}>
      <div className="divide-y divide-slate-100">
        <Toggle
          label={t('settings.searchable')}
          checked={settings.searchable}
          disabled={saving}
          onChange={(v) => save(v, settings.hidePresence)}
        />
        <Toggle
          label={t('settings.hidePresence')}
          checked={settings.hidePresence}
          disabled={saving}
          onChange={(v) => save(settings.searchable, v)}
        />
      </div>
      <p className="mt-3 text-xs text-slate-400">{t('settings.hidePresenceHint')}</p>
    </Section>
  )
}

// --- Jelszóváltás ---
function PasswordSection({ onChanged }: { onChanged: () => Promise<void> }) {
  const { t } = useTranslation()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [feedback, setFeedback] = useState<Feedback>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setFeedback(null)
    if (next.length < 8) {
      setFeedback({ kind: 'error', key: 'auth.validation.passwordShort' })
      return
    }
    setSubmitting(true)
    try {
      await changePassword(current, next)
      // Siker → a UI azonnal kilépteti a felhasználót (a refresh tokenek visszavonva).
      await onChanged()
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
      setSubmitting(false)
    }
  }

  return (
    <Section title={t('settings.changePassword')}>
      <form onSubmit={onSubmit} className="flex max-w-md flex-col gap-3">
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium text-slate-700">{t('settings.currentPassword')}</span>
          <input
            type="password"
            autoComplete="current-password"
            value={current}
            onChange={(e) => setCurrent(e.target.value)}
            className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 outline-none focus:border-brand focus:bg-white"
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium text-slate-700">{t('settings.newPassword')}</span>
          <input
            type="password"
            autoComplete="new-password"
            value={next}
            onChange={(e) => setNext(e.target.value)}
            className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 outline-none focus:border-brand focus:bg-white"
          />
        </label>
        {feedback && (
          <p className={`text-sm ${feedback.kind === 'ok' ? 'text-emerald-600' : 'text-rose-600'}`} role="alert">
            {t(feedback.key)}
          </p>
        )}
        <p className="text-xs text-slate-400">{t('settings.changePasswordHint')}</p>
        <button
          type="submit"
          disabled={submitting || !current || !next}
          className="self-start rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
        >
          {submitting ? t('settings.saving') : t('settings.changePassword')}
        </button>
      </form>
    </Section>
  )
}

// --- 2FA ---
function TwoFactorSection({
  settings,
  onChange,
}: {
  settings: Settings
  onChange: (s: Settings) => void
}) {
  const { t } = useTranslation()
  // Beállítás-folyamat állapotai.
  const [setup, setSetup] = useState<{ secret: string; qr: string } | null>(null)
  const [code, setCode] = useState('')
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null)
  const [disableMode, setDisableMode] = useState(false)
  const [busy, setBusy] = useState(false)
  const [feedback, setFeedback] = useState<Feedback>(null)

  async function startSetup() {
    setFeedback(null)
    setBusy(true)
    try {
      const res = await begin2faSetup()
      const qr = await QRCode.toDataURL(res.otpauthUri, { margin: 1, width: 200 })
      setSetup({ secret: res.secret, qr })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setBusy(false)
    }
  }

  async function confirmEnable(e: FormEvent) {
    e.preventDefault()
    setFeedback(null)
    setBusy(true)
    try {
      const res = await enable2fa(code.trim())
      setRecoveryCodes(res.recoveryCodes)
      setSetup(null)
      setCode('')
      onChange({ ...settings, totpEnabled: true })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setBusy(false)
    }
  }

  async function confirmDisable(e: FormEvent) {
    e.preventDefault()
    setFeedback(null)
    setBusy(true)
    try {
      await disable2fa(code.trim())
      setDisableMode(false)
      setCode('')
      onChange({ ...settings, totpEnabled: false })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setBusy(false)
    }
  }

  // A frissen generált helyreállító kódok (egyszer megjelenítve).
  if (recoveryCodes) {
    return (
      <Section title={t('settings.twoFactor')}>
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4">
          <p className="flex items-center gap-2 text-sm font-medium text-emerald-700">
            <ShieldCheck className="h-4 w-4" />
            {t('settings.twoFactorEnabled')}
          </p>
          <p className="mt-2 text-sm text-slate-700">{t('settings.recoveryCodesHint')}</p>
          <ul className="mt-3 grid grid-cols-2 gap-2 font-mono text-sm">
            {recoveryCodes.map((c) => (
              <li key={c} className="rounded bg-white px-2 py-1 text-center text-slate-800">
                {c}
              </li>
            ))}
          </ul>
          <button
            type="button"
            onClick={() => setRecoveryCodes(null)}
            className="mt-4 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-dark"
          >
            {t('settings.recoveryCodesDone')}
          </button>
        </div>
      </Section>
    )
  }

  return (
    <Section title={t('settings.twoFactor')}>
      <p className="mb-3 flex items-center gap-2 text-sm">
        {settings.totpEnabled ? (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-700">
            <ShieldCheck className="h-3.5 w-3.5" /> {t('settings.twoFactorOn')}
          </span>
        ) : (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500">
            <ShieldOff className="h-3.5 w-3.5" /> {t('settings.twoFactorOff')}
          </span>
        )}
      </p>
      <p className="mb-4 text-sm text-slate-500">{t('settings.twoFactorDesc')}</p>

      {feedback && (
        <p className="mb-3 text-sm text-rose-600" role="alert">
          {t(feedback.key)}
        </p>
      )}

      {/* Bekapcsolva → kikapcsolás kóddal */}
      {settings.totpEnabled ? (
        disableMode ? (
          <form onSubmit={confirmDisable} className="flex max-w-sm flex-col gap-3">
            <label className="flex flex-col gap-1 text-sm">
              <span className="font-medium text-slate-700">{t('settings.enterCodeToDisable')}</span>
              <input
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="000000"
                className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 tracking-widest outline-none focus:border-brand focus:bg-white"
              />
            </label>
            <div className="flex gap-2">
              <button
                type="submit"
                disabled={busy || !code.trim()}
                className="rounded-lg bg-rose-600 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-700 disabled:opacity-60"
              >
                {t('settings.disable2fa')}
              </button>
              <button
                type="button"
                onClick={() => {
                  setDisableMode(false)
                  setCode('')
                  setFeedback(null)
                }}
                className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100"
              >
                {t('settings.cancel')}
              </button>
            </div>
          </form>
        ) : (
          <button
            type="button"
            onClick={() => setDisableMode(true)}
            className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            {t('settings.disable2fa')}
          </button>
        )
      ) : setup ? (
        // Beállítás folyamatban: QR + titok + kód
        <form onSubmit={confirmEnable} className="flex max-w-sm flex-col gap-3">
          <p className="text-sm text-slate-600">{t('settings.scanQr')}</p>
          <img src={setup.qr} alt="2FA QR" className="h-48 w-48 self-center rounded-lg border border-slate-200" />
          <p className="text-xs text-slate-500">
            {t('settings.manualSecret')}: <code className="font-mono text-slate-800">{setup.secret}</code>
          </p>
          <label className="flex flex-col gap-1 text-sm">
            <span className="font-medium text-slate-700">{t('settings.enterCode')}</span>
            <input
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="000000"
              className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 tracking-widest outline-none focus:border-brand focus:bg-white"
            />
          </label>
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={busy || !code.trim()}
              className="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-60"
            >
              {t('settings.enable2fa')}
            </button>
            <button
              type="button"
              onClick={() => {
                setSetup(null)
                setCode('')
                setFeedback(null)
              }}
              className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100"
            >
              {t('settings.cancel')}
            </button>
          </div>
        </form>
      ) : (
        <button
          type="button"
          onClick={startSetup}
          disabled={busy}
          className="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-dark disabled:opacity-60"
        >
          {t('settings.enable2fa')}
        </button>
      )}
    </Section>
  )
}
