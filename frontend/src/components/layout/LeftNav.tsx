import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { NavLink } from 'react-router-dom'
import {
  CreditCard,
  FolderKanban,
  Image as ImageIcon,
  type LucideIcon,
  MessageCircle,
  Rss,
  Settings,
  User,
  UserRoundPlus,
  Users,
} from 'lucide-react'
import { useFriendNotifications } from '../../friends/FriendNotificationsContext'
import { useChat } from '../../chat/ChatContext'
import { AUTH_LOGOUT_EVENT } from '../../lib/api'
import { GROUPS_CHANGED_EVENT, getMyGroups } from '../../groups/groupsApi'
import type { Group } from '../../groups/types'

type NavItem = {
  to: string
  labelKey: string
  icon: LucideIcon
}

const PRIMARY: NavItem[] = [
  { to: '/', labelKey: 'nav.feed', icon: Rss },
  { to: '/friends', labelKey: 'nav.friends', icon: Users },
  { to: '/following', labelKey: 'nav.following', icon: UserRoundPlus },
  { to: '/groups', labelKey: 'nav.groups', icon: FolderKanban },
  { to: '/messages', labelKey: 'nav.messages', icon: MessageCircle },
  { to: '/profile', labelKey: 'nav.profile', icon: User },
  { to: '/media', labelKey: 'nav.media', icon: ImageIcon },
  { to: '/billing', labelKey: 'nav.subscription', icon: CreditCard },
  { to: '/settings', labelKey: 'nav.settings', icon: Settings },
]

type LeftNavProps = {
  /** Mobil drawerben: navigáció után zárjuk be a menüt. */
  onNavigate?: () => void
}

export default function LeftNav({ onNavigate }: LeftNavProps) {
  const { t } = useTranslation()
  const { unseenCount } = useFriendNotifications()
  const { totalUnread } = useChat()

  // A felhasználó csoportjai a „Csoportjaim" szekcióhoz. Best-effort: hibát itt nem mutatunk
  // (a /groups oldal igen). Csatlakozás/kilépés/létrehozás után a GROUPS_CHANGED_EVENT frissít.
  const [myGroups, setMyGroups] = useState<Group[]>([])

  useEffect(() => {
    let active = true
    const refresh = () => {
      getMyGroups()
        .then((groups) => {
          if (active) setMyGroups(groups)
        })
        .catch(() => {})
    }
    refresh()
    const onLogout = () => setMyGroups([])
    window.addEventListener(GROUPS_CHANGED_EVENT, refresh)
    window.addEventListener(AUTH_LOGOUT_EVENT, onLogout)
    return () => {
      active = false
      window.removeEventListener(GROUPS_CHANGED_EVENT, refresh)
      window.removeEventListener(AUTH_LOGOUT_EVENT, onLogout)
    }
  }, [])

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
      isActive
        ? 'bg-brand/10 text-brand'
        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
    }`

  return (
    <nav className="flex flex-col gap-1" aria-label={t('nav.feed')}>
      {PRIMARY.map(({ to, labelKey, icon: Icon }) => {
        // Badge: az „Ismerősök" mellett a megtekintetlen kérések, az „Üzenetek" mellett az
        // olvasatlan üzenetek száma; 0-nál nincs jelzés.
        const badge = to === '/friends' ? unseenCount : to === '/messages' ? totalUnread : 0
        return (
          <NavLink key={to} to={to} end={to === '/'} className={linkClass} onClick={onNavigate}>
            <Icon className="h-5 w-5 shrink-0" />
            <span>{t(labelKey)}</span>
            {badge > 0 && (
              <span
                className="ml-auto flex h-5 min-w-5 items-center justify-center rounded-full bg-rose-500 px-1.5 text-[11px] font-bold text-white"
                aria-label={t('nav.newFriendRequests', { count: badge })}
              >
                {badge}
              </span>
            )}
          </NavLink>
        )
      })}

      {myGroups.length > 0 && (
        <>
          <div className="mt-5 px-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
            {t('nav.myGroups')}
          </div>
          <div className="mt-1 flex flex-col gap-1">
            {myGroups.map((group) => (
              <NavLink
                key={group.id}
                to={`/groups/${group.id}`}
                className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900"
                onClick={onNavigate}
              >
                <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-slate-200 text-xs font-bold text-slate-600">
                  {group.name[0]?.toUpperCase()}
                </span>
                <span className="truncate">{group.name}</span>
              </NavLink>
            ))}
          </div>
        </>
      )}
    </nav>
  )
}
