import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { AuthProvider } from './store/authStore'
import RequireAuth from './components/RequireAuth'
import AppLayout from './components/AppLayout'
import LoginPage from './pages/auth/LoginPage'
import HomePage from './pages/student/HomePage'
import CoursePage from './pages/student/CoursePage'
import ExerciseListPage from './pages/student/ExerciseListPage'
import ExerciseDetailPage from './pages/student/ExerciseDetailPage'
import RecommendPage from './pages/student/RecommendPage'
import KnowledgeGraphPage from './pages/student/KnowledgeGraphPage'
import NotificationPage from './pages/student/NotificationPage'
import ProfilePage from './pages/student/ProfilePage'
import AnswerHistoryPage from './pages/student/AnswerHistoryPage'
import GradePage from './pages/teacher/GradePage'
import ExerciseManagePage from './pages/teacher/ExerciseManagePage'
import KnowledgeManagePage from './pages/teacher/KnowledgeManagePage'
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
              <Route path="exercises" element={<ExerciseListPage />} />
              <Route path="exercises/:id" element={<ExerciseDetailPage />} />
              <Route path="recommend" element={<RecommendPage />} />
              <Route path="knowledge" element={<KnowledgeGraphPage />} />
              <Route path="notifications" element={<NotificationPage />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="history" element={<AnswerHistoryPage />} />
              {/* 教师/管理员 */}
              <Route
                path="grade"
                element={
                  <RequireAuth roles={['TEACHER', 'ADMIN']}>
                    <GradePage />
                  </RequireAuth>
                }
              />
              <Route
                path="exercises-manage"
                element={
                  <RequireAuth roles={['TEACHER', 'ADMIN']}>
                    <ExerciseManagePage />
                  </RequireAuth>
                }
              />
              <Route
                path="knowledge-manage"
                element={
                  <RequireAuth roles={['TEACHER', 'ADMIN']}>
                    <KnowledgeManagePage />
                  </RequireAuth>
                }
              />
              {/* 管理员 */}
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
