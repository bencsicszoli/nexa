import type { ReactNode } from 'react'
import LanguageSwitcher from '../../components/LanguageSwitcher'

// Középre igazított kártya a bejelentkező/regisztrációs űrlapokhoz, Nexa-brandinggel.
export default function AuthLayout({
  title,
  subtitle,
  children,
}: {
  title: string
  subtitle: string
  children: ReactNode
}) {
  return (
    <div className="flex min-h-screen flex-col bg-gradient-to-br from-slate-100 to-brand/5">
      <header className="flex items-center justify-between px-4 py-4 sm:px-6">
        <div className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-brand to-brand-light text-lg font-extrabold text-white">
            N
          </span>
          <span className="text-lg font-extrabold text-brand">Nexa</span>
        </div>
        <LanguageSwitcher />
      </header>

      <main className="flex flex-1 items-center justify-center px-4 py-8">
        <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <h1 className="text-2xl font-extrabold text-slate-900">{title}</h1>
          <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
          <div className="mt-6">{children}</div>
        </div>
      </main>
    </div>
  )
}
