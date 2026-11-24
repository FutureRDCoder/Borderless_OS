import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  currentUser,
  login as apiLogin,
  logout as apiLogout,
  signup as apiSignup,
  type SignupInput,
} from '../api/client'
import type { AuthUser, Role } from '../api/types'

interface AuthContextValue {
  user: AuthUser | null
  initialising: boolean
  primaryRole: Role | null
  roles: Role[]
  signIn: (email: string, password: string) => Promise<void>
  signUp: (input: SignupInput) => Promise<void>
  signOut: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

function normaliseRole(role: string): Role {
  return role.replace(/^ROLE_/, '') as Role
}

function rolesOf(user: AuthUser | null): Role[] {
  if (!user) {
    return []
  }

  return [...new Set(user.roles.map(normaliseRole))]
}

function primaryRoleOf(user: AuthUser | null): Role | null {
  const roles = rolesOf(user)

  if (roles.includes('CUSTOMER')) return 'CUSTOMER'
  if (roles.includes('OPERATIONS')) return 'OPERATIONS'
  if (roles.includes('COMPLIANCE')) return 'COMPLIANCE'
  if (roles.includes('ADMIN')) return 'ADMIN'
  return null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [initialising, setInitialising] = useState(true)

  useEffect(() => {
    currentUser()
      .then((nextUser) => setUser(nextUser))
      .catch(() => setUser(null))
      .finally(() => setInitialising(false))
  }, [])

  const signIn = useCallback(async (email: string, password: string) => {
    await apiLogin(email, password)
    setUser(await currentUser())
  }, [])

  const signUp = useCallback(async (input: SignupInput) => {
    await apiSignup(input)
    setUser(await currentUser())
  }, [])

  const signOut = useCallback(async () => {
    try {
      await apiLogout()
    } finally {
      setUser(null)
    }
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    initialising,
    primaryRole: primaryRoleOf(user),
    roles: rolesOf(user),
    signIn,
    signUp,
    signOut,
  }), [user, initialising, signIn, signUp, signOut])

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }

  return context
}
