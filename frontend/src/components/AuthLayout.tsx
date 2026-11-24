import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { BrandMark, Icon } from './Icon'

const benefits = [
  {
    title: 'Operations-ready workflows',
    detail: 'Review queues, information requests, and provider outcomes.',
  },
  {
    title: 'Compliance controls built in',
    detail: 'Destination review holds with reasoned, auditable decisions.',
  },
  {
    title: 'Complete transfer visibility',
    detail: 'Every status change is recorded in the audit timeline.',
  },
]

export function AuthLayout({
  children,
  wide = false,
}: {
  children: ReactNode
  wide?: boolean
}) {
  return (
    <div className="auth-shell">
      <aside className="auth-aside">
        <Link className="brand brand-inverse" to="/login">
          <BrandMark />
          <span>
            <strong>BorderlessOS</strong>
            <small>Global remittance operations</small>
          </span>
        </Link>

        <div className="auth-story">
          <span className="auth-kicker">
            <Icon name="spark" size={14} />
            Cross-border payments, under control
          </span>
          <h1>Send money with clarity at every step.</h1>
          <p>
            A realistic remittance and compliance workspace for customer
            transfers, operations review, and destination-risk decisions.
          </p>

          <ul className="auth-benefits">
            {benefits.map((benefit) => (
              <li key={benefit.title}>
                <span className="auth-check">
                  <Icon name="check" size={14} />
                </span>
                <span>
                  <strong>{benefit.title}</strong>
                  <small>{benefit.detail}</small>
                </span>
              </li>
            ))}
          </ul>
        </div>

        <div className="auth-trust">
          <div>
            <strong>11</strong>
            <span>Corridors</span>
          </div>
          <div>
            <strong>4</strong>
            <span>Workspaces</span>
          </div>
          <div>
            <strong>24/7</strong>
            <span>Visibility</span>
          </div>
        </div>

        <p className="auth-disclaimer">
          <Icon name="shield" size={15} />
          Simulation environment. No real funds are moved, held, disbursed, or
          settled.
        </p>
      </aside>

      <main className="auth-main">
        <div className={wide ? 'auth-card-wrap auth-card-wrap-wide' : 'auth-card-wrap'}>
          {children}
        </div>
        <p className="auth-legal">
          © {new Date().getFullYear()} BorderlessOS · Educational prototype
        </p>
      </main>
    </div>
  )
}
