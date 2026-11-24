import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import type {
  AdminUser,
  AuditEvent,
  OperationsTransfer,
} from '../../api/types'
import { ErrorAlert } from '../../components/ErrorAlert'
import { Loading } from '../../components/Loading'
import { Pagination, usePagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime, formatMoney, humanizeKey } from '../../utils/format'

export function AdminDashboard() {
  const [dashboard, setDashboard] = useState<Record<string, number>>({})
  const [users, setUsers] = useState<AdminUser[]>([])
  const [transfers, setTransfers] = useState<OperationsTransfer[]>([])
  const [auditEvents, setAuditEvents] = useState<AuditEvent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const userPages = usePagination(users)
  const transferPages = usePagination(transfers)
  const auditPages = usePagination(auditEvents, 5)

  async function load() {
    setLoading(true)
    try {
      const [nextDashboard, nextUsers, nextTransfers, nextAudit] =
        await Promise.all([
          api<Record<string, number>>('/api/v1/admin/dashboard'),
          api<AdminUser[]>('/api/v1/admin/users'),
          api<OperationsTransfer[]>('/api/v1/admin/transfers'),
          api<AuditEvent[]>('/api/v1/admin/audit-events'),
        ])

      setDashboard(nextDashboard)
      setUsers(nextUsers)
      setTransfers(nextTransfers)
      setAuditEvents(nextAudit)
    } catch (nextError) {
      setError(nextError)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  if (loading) {
    return <Loading label="Loading administration overview…" />
  }

  return (
    <div className="page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Administration workspace</p>
          <h1>Read-only oversight</h1>
          <p className="muted">
            Admin can inspect demo accounts, transfers, and authorised audit
            information. Admin cannot clear holds, approve transfers, or
            override workflow state.
          </p>
        </div>
        <button className="button" onClick={load}>Refresh</button>
      </div>

      <ErrorAlert error={error} />

      <section className="metric-grid compact">
        {Object.entries(dashboard).map(([key, value]) => (
          <div className="metric-card" key={key}>
            <span className="muted small">{humanizeKey(key)}</span>
            <strong>{value}</strong>
          </div>
        ))}
      </section>

      <div className="split">
        <section className="card">
          <h2>Demo users</h2>
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Email</th>
                    <th>Display name</th>
                    <th>Roles</th>
                    <th>Enabled</th>
                  </tr>
                </thead>
                <tbody>
                  {userPages.items.map((user) => (
                    <tr key={user.id}>
                      <td>{user.email}</td>
                      <td>{user.displayName}</td>
                      <td>{user.roles.join(', ')}</td>
                      <td>{user.enabled ? 'Yes' : 'No'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              state={userPages.state}
              onPageChange={userPages.setPage}
            />
          </>
        </section>

        <section className="card">
          <h2>Recent audit events</h2>
          {auditEvents.length === 0 ? (
            <p className="muted">No audit events recorded.</p>
          ) : (
            <>
              <ol className="timeline compact">
                {auditPages.items.map((event) => (
                  <li key={event.id}>
                    <span className="timeline-dot" />
                    <div>
                      <strong>{event.summary}</strong>
                      <p className="muted small">
                        {event.eventType} · {event.actorRole} ·{' '}
                        {formatDateTime(event.createdAt)}
                      </p>
                    </div>
                  </li>
                ))}
              </ol>
              <Pagination
                state={auditPages.state}
                onPageChange={auditPages.setPage}
              />
            </>
          )}
        </section>
      </div>

      <section className="card">
        <h2>Recent transfers</h2>
        {transfers.length === 0 ? (
          <p className="muted">No transfers recorded.</p>
        ) : (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Transfer</th>
                    <th>Destination</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {transferPages.items.map((transfer) => (
                    <tr key={transfer.id}>
                      <td>
                        <strong>{transfer.recipientName}</strong>
                        <small className="muted block mono">
                          {transfer.id.slice(0, 8)}
                        </small>
                      </td>
                      <td>
                        {transfer.destinationCountry} /{' '}
                        {transfer.destinationCurrency}
                      </td>
                      <td>
                        {formatMoney(
                          transfer.sourceAmount,
                          transfer.sourceCurrency,
                        )}
                      </td>
                      <td><StatusBadge status={transfer.status} /></td>
                      <td>{formatDateTime(transfer.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              state={transferPages.state}
              onPageChange={transferPages.setPage}
            />
          </>
        )}
      </section>
    </div>
  )
}
