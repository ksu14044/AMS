import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function ProtectedRoute({ allowedRoles }) {
  const { user, isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (user.status === 'PENDING') {
    return <Navigate to="/pending" replace />
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}

export function GuestRoute() {
  const { isAuthenticated, homePath, user } = useAuth()
  if (isAuthenticated) {
    if (user?.status === 'PENDING') {
      return <Navigate to="/pending" replace />
    }
    return <Navigate to={homePath} replace />
  }
  return <Outlet />
}
