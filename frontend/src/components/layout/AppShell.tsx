import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { X } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import TopBar from './TopBar'
import LeftNav from './LeftNav'
import RightSidebar from './RightSidebar'
import { FriendNotificationsProvider } from '../../friends/FriendNotificationsContext'
import { StompProvider } from '../../realtime/StompProvider'
import { NotificationsProvider } from '../../notifications/NotificationsContext'
import { ChatProvider } from '../../chat/ChatContext'
import { CallProvider } from '../../call/CallContext'
import CallOverlay from '../../call/CallOverlay'
import NotificationToaster from '../NotificationToaster'
import RequireSubscription from '../../subscription/RequireSubscription'
import DevSubscriptionPanel from '../../subscription/DevSubscriptionPanel'

// Az "A" (3 oszlopos) elrendezés: bal navigáció · középső tartalom · jobb sáv.
// Reszponzív viselkedés:
//  - lg alatt a bal navigáció becsukódik és hamburger-gombbal nyíló drawer lesz,
//  - xl alatt a jobb sáv eltűnik.
export default function AppShell() {
  const { t } = useTranslation()
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <FriendNotificationsProvider>
    <StompProvider>
    <NotificationsProvider>
    <ChatProvider>
    <CallProvider>
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <TopBar onOpenMenu={() => setMenuOpen(true)} />

      <div className="mx-auto flex max-w-7xl gap-6 px-3 py-6 sm:px-4">
        {/* Bal navigáció — fix oszlop lg-től felfelé */}
        <aside className="hidden w-60 shrink-0 lg:block">
          <div className="sticky top-20">
            <LeftNav />
          </div>
        </aside>

        {/* Középső tartalom — előfizetés-gating mögött (#15); a /billing kivétel */}
        <main className="min-w-0 flex-1">
          <RequireSubscription>
            <Outlet />
          </RequireSubscription>
        </main>

        {/* Jobb sáv — xl-től felfelé */}
        <aside className="hidden w-80 shrink-0 xl:block">
          <div className="sticky top-20">
            <RightSidebar />
          </div>
        </aside>
      </div>

      {/* Mobil drawer a bal navigációhoz */}
      {menuOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div
            className="absolute inset-0 bg-slate-900/40"
            onClick={() => setMenuOpen(false)}
            aria-hidden="true"
          />
          <div className="absolute left-0 top-0 h-full w-72 overflow-y-auto bg-white p-4 shadow-xl">
            <div className="mb-4 flex items-center justify-between">
              <span className="text-lg font-extrabold text-brand">Nexa</span>
              <button
                type="button"
                onClick={() => setMenuOpen(false)}
                className="rounded-lg p-2 text-slate-600 hover:bg-slate-100"
                aria-label={t('topbar.openMenu')}
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <LeftNav onNavigate={() => setMenuOpen(false)} />
          </div>
        </div>
      )}

      {/* Valós idejű értesítés toastja (#11) */}
      <NotificationToaster />

      {/* Videohívás teljes képernyős felülete (#13) — bárhol a felületen megjelenik */}
      <CallOverlay />

      {/* Dev/demo: előfizetés-állapot kapcsoló (#15) — csak dev-buildben és ha a backend dev-flag aktív;
          a paywallon kívül, hogy állapotváltáshoz mindig elérhető legyen */}
      <DevSubscriptionPanel />
    </div>
    </CallProvider>
    </ChatProvider>
    </NotificationsProvider>
    </StompProvider>
    </FriendNotificationsProvider>
  )
}
