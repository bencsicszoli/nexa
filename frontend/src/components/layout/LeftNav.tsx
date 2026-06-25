import { useTranslation } from 'react-i18next'
import { NavLink } from 'react-router-dom'
import {
  FolderKanban,
  Image as ImageIcon,
  type LucideIcon,
  Rss,
  Settings,
  User,
  Users,
} from 'lucide-react'
import { useFriendNotifications } from '../../friends/FriendNotificationsContext'

type NavItem = {
  to: string
  labelKey: string
  icon: LucideIcon
}

const PRIMARY: NavItem[] = [
  { to: '/', labelKey: 'nav.feed', icon: Rss },
  { to: '/friends', labelKey: 'nav.friends', icon: Users },
  { to: '/groups', labelKey: 'nav.groups', icon: FolderKanban },
  { to: '/profile', labelKey: 'nav.profile', icon: User },
  { to: '/media', labelKey: 'nav.media', icon: ImageIcon },
  { to: '/settings', labelKey: 'nav.settings', icon: Settings },
]

// Placeholder tag-csoportok — a valódi adat a #9 kártyával érkezik.
const MY_GROUPS = ['Fotósok köre', 'Kódolás HU', 'Receptek']

type LeftNavProps = {
  /** Mobil drawerben: navigáció után zárjuk be a menüt. */
  onNavigate?: () => void
}

export default function LeftNav({ onNavigate }: LeftNavProps) {
  const { t } = useTranslation()
  const { unseenCount } = useFriendNotifications()

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
      isActive
        ? 'bg-brand/10 text-brand'
        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
    }`

  return (
    <nav className="flex flex-col gap-1" aria-label={t('nav.feed')}>
      {PRIMARY.map(({ to, labelKey, icon: Icon }) => {
        // Az „Ismerősök" mellett a megtekintetlen kérések száma (badge); 0-nál nincs jelzés.
        const badge = to === '/friends' ? unseenCount : 0
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

      <div className="mt-5 px-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
        {t('nav.myGroups')}
      </div>
      <div className="mt-1 flex flex-col gap-1">
        {MY_GROUPS.map((group) => (
          <NavLink
            key={group}
            to="/groups"
            className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900"
            onClick={onNavigate}
          >
            <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-slate-200 text-xs font-bold text-slate-600">
              {group[0]}
            </span>
            <span className="truncate">{group}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
