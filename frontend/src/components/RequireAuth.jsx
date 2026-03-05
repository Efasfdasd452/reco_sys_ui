import { Navigate } from 'react-router-dom'
import { useAuth } from '../store/authStore'

export default function RequireAuth({ children, roles }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}
