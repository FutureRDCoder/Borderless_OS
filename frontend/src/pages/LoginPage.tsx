import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'
import { ErrorAlert } from '../components/ErrorAlert'
import { Icon } from '../components/Icon'

const DEMO_PASSWORD = 'BorderlessDemo123!'

const demoAccounts = [
  {
    role: 'CUSTOMER',
    label: 'Customer',
    email: 'customer@borderlessos.local',
  },
  {
    role: 'OPERATIONS',
    label: 'Operations',
    email: 'operations@borderlessos.local',
  },
  {
    role: 'COMPLIANCE',
    label: 'Compliance',
    email: 'compliance@borderlessos.local',
  },
  {
    role: 'ADMIN',
    label: 'Admin',
    email: 'admin@borderlessos.local',
  },
]

export function LoginPage() {
  const { user, signIn, initialising } = useAuth()
  const navigate = useNavigate()
  const [selectedRole, setSelectedRole] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!initialising && user) {
      navigate('/', { replace: true })
    }
  }, [initialising, user, navigate])

  if (!initialising && user) {
    return <Navigate to="/" replace />
  }

  function chooseRole(role: string) {
    setSelectedRole(role)
    setError(null)

    if (!role) {
      setEmail('')
      setPassword('')
      return
    }

    const account = demoAccounts.find((item) => item.role === role)

    if (account) {
      setEmail(account.email)
      setPassword(DEMO_PASSWORD)
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setBusy(true)

    try {
      await signIn(email, password)
      navigate('/', { replace: true })
    } catch (nextError) {
      setError(nextError)
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout>
      <div className="auth-card">
        <div className="auth-card-header">
          <span className="auth-icon">
            <Icon name="lock" size={20} />
          </span>
          <div>
            <p className="eyebrow">Secure access</p>
            <h2>Welcome back</h2>
            <p className="muted">
              Sign in to your BorderlessOS workspace.
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="stack">
          <label>
            Demo role
            <select
              value={selectedRole}
              onChange={(event) => chooseRole(event.target.value)}
            >
              <option value="">Select a role</option>
              {demoAccounts.map((account) => (
                <option key={account.role} value={account.role}>
                  {account.label}
                </option>
              ))}
            </select>
          </label>

          <label>
            Work email
            <input
              type="email"
              autoComplete="username"
              placeholder="you@company.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </label>

          <label>
            Password
            <input
              type="password"
              autoComplete="current-password"
              placeholder="Enter your password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </label>

          <ErrorAlert error={error} />

          <button className="button primary button-wide" disabled={busy}>
            {busy ? 'Signing in…' : 'Sign in'}
            {!busy && <Icon name="arrow-right" size={17} />}
          </button>
        </form>

        <p className="auth-switch">
          New to BorderlessOS? <Link to="/signup">Create an account</Link>
        </p>
      </div>
    </AuthLayout>
  )
}
