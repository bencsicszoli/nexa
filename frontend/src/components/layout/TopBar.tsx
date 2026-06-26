import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Home, Menu, MessageCircle, Search } from 'lucide-react'
import LanguageSwitcher from '../LanguageSwitcher'
import UserMenu from './UserMenu'
import NotificationBell from '../NotificationBell'

type TopBarProps = {
  /** Mobil bal-navigáció (drawer) megnyitása. */
  onOpenMenu: () => void
}

export default function TopBar({ onOpenMenu }: TopBarProps) {
  const { t } = useTranslation()

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white">
      <div className="flex h-14 items-center gap-3 px-3 sm:px-4">
        {/* Hamburger — csak mobilon, a bal navigáció előhívásához */}
        <button
          type="button"
          onClick={onOpenMenu}
          className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
          aria-label={t('topbar.openMenu')}
        >
          <Menu className="h-5 w-5" />
        </button>

        {/* Logó */}
        <Link to="/" className="flex shrink-0 items-center gap-2">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-brand to-brand-light text-lg font-extrabold text-white">
            N
          </span>
          <span className="hidden text-lg font-extrabold text-brand sm:inline">Nexa</span>
        </Link>

        {/* Kereső */}
        <div className="relative mx-auto w-full max-w-xl">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="search"
            placeholder={t('topbar.searchPlaceholder')}
            className="w-full rounded-full border border-slate-200 bg-slate-50 py-2 pl-9 pr-4 text-sm outline-none transition-colors placeholder:text-slate-400 focus:border-brand focus:bg-white"
          />
        </div>

        {/* Jobb oldali ikonsor */}
        <div className="flex shrink-0 items-center gap-1 sm:gap-2">
          <button
            type="button"
            className="hidden rounded-full p-2 text-slate-600 hover:bg-slate-100 sm:inline-flex"
            aria-label={t('topbar.home')}
          >
            <Home className="h-5 w-5" />
          </button>
          <button
            type="button"
            className="rounded-full p-2 text-slate-600 hover:bg-slate-100"
            aria-label={t('topbar.messages')}
          >
            <MessageCircle className="h-5 w-5" />
          </button>
          <NotificationBell />

          <div className="mx-1 hidden sm:block">
            <LanguageSwitcher />
          </div>

          <UserMenu />
        </div>
      </div>

      {/* Nyelvváltó mobilon külön sorban, hogy a fejléc ne legyen zsúfolt */}
      <div className="flex justify-end border-t border-slate-100 px-3 py-1.5 sm:hidden">
        <LanguageSwitcher />
      </div>
    </header>
  )
}
