import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout } from '../components/AuthLayout'
import { ErrorAlert } from '../components/ErrorAlert'
import { Icon } from '../components/Icon'

export function SignupPage() {
  const { user, signUp, initialising } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
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

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)

    if (password.length < 12) {
      setError(new Error('Use at least 12 characters for your password.'))
      return
    }

    if (password !== confirmPassword) {
      setError(new Error('Passwords do not match.'))
      return
    }

    setBusy(true)

    try {
      await signUp({
        displayName: displayName.trim(),
        email: email.trim(),
        password,
      })
      navigate('/customer', { replace: true })
    } catch (nextError) {
      setError(nextError)
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout>
      <div className="auth-card">
        <Link className="auth-back" to="/login">
          <Icon name="arrow-right" size={14} />
          Choose account type
        </Link>

        <div className="auth-card-header">
          <span className="auth-icon">
            <Icon name="users" size={20} />
          </span>
          <div>
            <p className="eyebrow">Customer account</p>
            <h2>Create your account</h2>
            <p className="muted">
              Start sending transfers in a guided simulation environment.
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="stack">
          <label>
            Full name
            <input
              type="text"
              autoComplete="name"
              placeholder="Your full name"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              maxLength={120}
              required
            />
          </label>

          <label>
            Email address
            <input
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              maxLength={255}
              required
            />
          </label>

          <div className="field-row">
            <label>
              Password
              <input
                type="password"
                autoComplete="new-password"
                placeholder="At least 12 characters"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                minLength={12}
                maxLength={128}
                required
              />
            </label>

            <label>
              Confirm password
              <input
                type="password"
                autoComplete="new-password"
                placeholder="Repeat your password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                minLength={12}
                maxLength={128}
                required
              />
            </label>
          </div>

          <p className="form-hint">
            Use 12 or more characters. Customer accounts are created with
            transfer-only access.
          </p>

          <ErrorAlert error={error} />

          <button className="button primary button-wide" disabled={busy}>
            {busy ? 'Creating account…' : 'Create account'}
            {!busy && <Icon name="arrow-right" size={17} />}
          </button>
        </form>

        <p className="auth-switch">
          Already have an account? <Link to="/login/customer">Sign in</Link>
        </p>

        <p className="auth-fineprint">
          By creating an account you acknowledge that BorderlessOS is an
          educational prototype. No real funds are moved or held.
        </p>
      </div>
    </AuthLayout>
  )
}
