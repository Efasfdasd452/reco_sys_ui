import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { AuthProvider } from './store/authStore'
import RequireAuth from './components/RequireAuth'
import AppLayout from './components/AppLayout'
import LoginPage from './pages/auth/LoginPage'
import HomePage from './pages/student/HomePage'
import CoursePage from './pages/student/CoursePage'
import RecommendPage from './pages/student/RecommendPage'
import KnowledgeGraphPage from './pages/student/KnowledgeGraphPage'
import NotificationPage from './pages/student/NotificationPage'
import GradePage from './pages/teacher/GradePage'
import AdminPage from './pages/admin/AdminPage'
import './i18n'

export default function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/"
              element={
                <RequireAuth>
                  <AppLayout />
                </RequireAuth>
              }
            >
              <Route index element={<HomePage />} />
              <Route path="courses" element={<CoursePage />} />
              <Route path="recommend" element={<RecommendPage />} />
              <Route path="knowledge" element={<KnowledgeGraphPage />} />
              <Route path="notifications" element={<NotificationPage />} />
              <Route
                path="grade"
                element={
                  <RequireAuth roles={['TEACHER', 'ADMIN']}>
                    <GradePage />
                  </RequireAuth>
                }
              />
              <Route
                path="admin"
                element={
                  <RequireAuth roles={['ADMIN']}>
                    <AdminPage />
                  </RequireAuth>
                }
              />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ConfigProvider>
  )
}
