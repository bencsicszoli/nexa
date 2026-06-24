import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from './components/layout/AppShell'
import FeedPage from './pages/FeedPage'
import PlaceholderPage from './pages/PlaceholderPage'

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<FeedPage />} />
        <Route path="friends" element={<PlaceholderPage titleKey="nav.friends" />} />
        <Route path="groups" element={<PlaceholderPage titleKey="nav.groups" />} />
        <Route path="profile" element={<PlaceholderPage titleKey="nav.profile" />} />
        <Route path="media" element={<PlaceholderPage titleKey="nav.media" />} />
        <Route path="settings" element={<PlaceholderPage titleKey="nav.settings" />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
