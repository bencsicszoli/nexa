import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from './components/layout/AppShell'
import ProtectedRoute from './auth/ProtectedRoute'
import FeedPage from './pages/FeedPage'
import PlaceholderPage from './pages/PlaceholderPage'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'

export default function App() {
  return (
    <Routes>
      {/* Publikus oldalak */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Védett oldalak — bejelentkezés nélkül a /login-ra irányít */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route index element={<FeedPage />} />
          <Route path="friends" element={<PlaceholderPage titleKey="nav.friends" />} />
          <Route path="groups" element={<PlaceholderPage titleKey="nav.groups" />} />
          <Route path="profile" element={<PlaceholderPage titleKey="nav.profile" />} />
          <Route path="media" element={<PlaceholderPage titleKey="nav.media" />} />
          <Route path="settings" element={<PlaceholderPage titleKey="nav.settings" />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Route>
    </Routes>
  )
}
