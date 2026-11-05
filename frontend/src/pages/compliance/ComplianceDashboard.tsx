import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'
import type { ComplianceDecision, ComplianceHold } from '../../api/types'
import { ErrorAlert } from '../../components/ErrorAlert'
import { Loading } from '../../components/Loading'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime, formatMoney } from '../../utils/format'

export function ComplianceDashboard() {
  const [holds, setHolds] = useState<ComplianceHold[]>([])
  const [decisions, setDecisions] = useState<ComplianceDecision[]>([])
  const [activeHolds, setActiveHolds] = useState(0)
  const [manualTransferId, setManualTransferId] = useState('')
  const [manualReason, setManualReason] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function load() {
    setLoading(true)
    try {
      const [nextHolds, nextDashboard, nextDecisions] = await Promise.all([
        api<ComplianceHold[]>('/api/v1/compliance/queue'),
        api<Record<string, number>>('/api/v1/compliance/dashboard'),
        api<ComplianceDecision[]>('/api/v1/compliance/audit-events'),
      ])
      setHolds(nextHolds)
      setActiveHolds(nextDashboard.activeHolds ?? 0)
      setDecisions(nextDecisions)
    } catch (nextError) {
      setError(nextError)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function placeManualHold(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setBusy(true)

    try {
      await api(`/api/v1/compliance/transfers/${manualTransferId}/place-hold`, {
        method: 'POST',
        body: { reason: manualReason },
      })
      setManualTransferId('')
      setManualReason('')
      await load()
    } catch (nextError) {
      setError(nextError)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Compliance workspace</p>
          <h1>Active holds</h1>
          <p className="muted">
            Compliance decisions require the COMPLIANCE role and a non-blank
            reason. Clearing a hold returns the transfer to operations review.
          </p>
        </div>
        <button className="button" onClick={load}>Refresh</button>
      </div>

      <ErrorAlert error={error} />

      <section className="metric-grid compact">
        <div className="metric-card">
          <span className="muted small">Active holds</span>
          <strong>{activeHolds}</strong>
        </div>
        <div className="metric-card">
          <span className="muted small">Recent decisions</span>
          <strong>{decisions.length}</strong>
        </div>
      </section>

      <section className="card">
        <div className="card-heading">
          <h2>Compliance queue</h2>
        </div>

        {loading ? (
          <Loading />
        ) : holds.length === 0 ? (
          <p className="muted">No active compliance holds.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Transfer</th>
                  <th>Destination</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Opened</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {holds.map((hold) => (
                  <tr key={hold.holdId}>
                    <td>
                      <strong>{hold.recipientName}</strong>
                      <small className="muted block mono">
                        {hold.transferId.slice(0, 8)}
                      </small>
                    </td>
                    <td>{hold.destinationCountry} / {hold.destinationCurrency}</td>
                    <td>
                      {formatMoney(
                        hold.sourceAmount,
                        hold.sourceCurrency,
                      )}
                    </td>
                    <td><StatusBadge status={hold.transferStatus} /></td>
                    <td>{formatDateTime(hold.openedAt)}</td>
                    <td>
                      <Link
                        className="button small"
                        to={`/compliance/holds/${hold.transferId}`}
                      >
                        Review hold
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <div className="split">
        <section className="card">
          <h2>Place a manual compliance hold</h2>
          <p className="muted small">
            The transfer must currently be UNDER_REVIEW and must not already
            have an active hold.
          </p>
          <form className="stack" onSubmit={placeManualHold}>
            <label>
              Transfer ID
              <input
                className="mono"
                value={manualTransferId}
                onChange={(event) => setManualTransferId(event.target.value)}
                required
              />
            </label>
            <label>
              Reason
              <textarea
                value={manualReason}
                onChange={(event) => setManualReason(event.target.value)}
                maxLength={1000}
                rows={3}
                required
              />
            </label>
            <button className="button danger" disabled={busy}>
              {busy ? 'Placing hold…' : 'Place hold'}
            </button>
          </form>
        </section>

        <section className="card">
          <h2>Recent decisions</h2>
          {decisions.length === 0 ? (
            <p className="muted">No compliance decisions recorded.</p>
          ) : (
            <ul className="plain-list">
              {decisions.slice(0, 20).map((decision) => (
                <li key={decision.id}>
                  <strong>{decision.decisionType}</strong>
                  <p className="muted small">
                    {decision.actorEmail} ·{' '}
                    {formatDateTime(decision.createdAt)}
                  </p>
                  {decision.reason && (
                    <p className="small">Reason: {decision.reason}</p>
                  )}
                  <Link
                    className="text-link small"
                    to={`/compliance/holds/${decision.transferId}`}
                  >
                    View transfer
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
