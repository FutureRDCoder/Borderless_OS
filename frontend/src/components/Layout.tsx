import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { BrandMark, Icon, type IconName } from './Icon'

interface NavLinkItem {
  to: string
  label: string
  icon: IconName
}

function navLinksFor(role: string | null): NavLinkItem[] {
  switch (role) {
    case 'CUSTOMER':
      return [
        { to: '/customer', label: 'Overview', icon: 'home' },
        { to: '/customer/beneficiaries', label: 'Beneficiaries', icon: 'users' },
        { to: '/customer/transfers/new', label: 'New transfer', icon: 'send' },
      ]
    case 'OPERATIONS':
      return [{ to: '/operations', label: 'Review queue', icon: 'inbox' }]
    case 'COMPLIANCE':
      return [{ to: '/compliance', label: 'Compliance holds', icon: 'shield' }]
    case 'ADMIN':
      return [{ to: '/admin', label: 'Administration', icon: 'building' }]
    default:
      return []
  }
}

const roleLabels: Record<string, string> = {
  CUSTOMER: 'Customer',
  OPERATIONS: 'Operations',
  COMPLIANCE: 'Compliance',
  ADMIN: 'Admin',
}

export function Layout() {
  const { user, primaryRole, signOut } = useAuth()
  const navigate = useNavigate()

  async function handleSignOut() {
    await signOut()
    navigate('/login', { replace: true })
  }

  const initials = user?.email.slice(0, 2).toUpperCase() ?? 'BO'
  const roleLabel = primaryRole ? roleLabels[primaryRole] : 'Workspace'

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link to="/" className="brand sidebar-brand">
          <BrandMark />
          <span>
            <strong>BorderlessOS</strong>
            <small>Global remittance operations</small>
          </span>
        </Link>

        <div className="workspace-badge">
          <span className="live-dot" />
          {roleLabel} workspace
        </div>

        <nav className="side-nav" aria-label="Workspace navigation">
          <p className="side-nav-label">Workspace</p>
          {navLinksFor(primaryRole).map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                isActive ? 'side-link active' : 'side-link'}
            >
              <Icon name={link.icon} size={18} />
              <span>{link.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="simulation-card">
            <span className="simulation-icon">
              <Icon name="shield" size={16} />
            </span>
            <div>
              <strong>Simulation mode</strong>
              <small>No real funds move in this environment.</small>
            </div>
          </div>

          <div className="account-card">
            <span className="avatar">{initials}</span>
            <div className="account-copy">
              <strong>{user?.email}</strong>
              <span className={`role-pill role-${primaryRole?.toLowerCase()}`}>
                {roleLabel}
              </span>
            </div>
            <button
              className="icon-button"
              onClick={handleSignOut}
              aria-label="Sign out"
              title="Sign out"
              type="button"
            >
              <Icon name="logout" size={18} />
            </button>
          </div>
        </div>
      </aside>

      <div className="app-main">
        <main className="content">
          <Outlet />
        </main>

        <footer className="footer">
          <span>BorderlessOS</span>
          <span>
            Educational prototype. No real funds are moved, held, disbursed,
            or settled.
          </span>
        </footer>
      </div>
    </div>
  )
}
