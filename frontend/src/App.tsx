import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from './components/layout/AppShell'
import ProtectedRoute from './auth/ProtectedRoute'
import FeedPage from './pages/FeedPage'
import ProfilePage from './pages/ProfilePage'
import UserProfilePage from './pages/UserProfilePage'
import FriendsPage from './pages/FriendsPage'
import FollowingPage from './pages/FollowingPage'
import GroupsPage from './pages/GroupsPage'
import GroupPage from './pages/GroupPage'
import MessagesPage from './pages/MessagesPage'
import BillingPage from './pages/BillingPage'
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
          <Route path="friends" element={<FriendsPage />} />
          <Route path="following" element={<FollowingPage />} />
          <Route path="groups" element={<GroupsPage />} />
          <Route path="groups/:groupId" element={<GroupPage />} />
          <Route path="messages" element={<MessagesPage />} />
          <Route path="messages/:conversationId" element={<MessagesPage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="users/:id" element={<UserProfilePage />} />
          <Route path="media" element={<PlaceholderPage titleKey="nav.media" />} />
          <Route path="billing" element={<BillingPage />} />
          <Route path="settings" element={<PlaceholderPage titleKey="nav.settings" />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Route>
    </Routes>
  )
}
