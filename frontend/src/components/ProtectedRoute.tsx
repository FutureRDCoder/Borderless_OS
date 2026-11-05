import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import type { Role } from '../api/types'
import { Loading } from './Loading'

export function ProtectedRoute({ allow }: { allow: Role[] }) {
  const { user, primaryRole, initialising } = useAuth()

  if (initialising) {
    return <Loading label="Checking your session…" />
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!primaryRole || !allow.includes(primaryRole)) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}

export function HomeRedirect() {
  const { user, primaryRole, initialising } = useAuth()

  if (initialising) {
    return <Loading label="Loading BorderlessOS…" />
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  switch (primaryRole) {
    case 'CUSTOMER':
      return <Navigate to="/customer" replace />
    case 'OPERATIONS':
      return <Navigate to="/operations" replace />
    case 'COMPLIANCE':
      return <Navigate to="/compliance" replace />
    case 'ADMIN':
      return <Navigate to="/admin" replace />
    default:
      return <Navigate to="/login" replace />
  }
}
