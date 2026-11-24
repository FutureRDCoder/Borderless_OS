import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'
import type {
  ComplianceDecision,
  ComplianceHold,
  ComplianceReviewTransfer,
} from '../../api/types'
import { ErrorAlert } from '../../components/ErrorAlert'
import { Loading } from '../../components/Loading'
import { Pagination, usePagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime, formatMoney } from '../../utils/format'

export function ComplianceDashboard() {
  const [holds, setHolds] = useState<ComplianceHold[]>([])
  const [reviewQueue, setReviewQueue] = useState<ComplianceReviewTransfer[]>([])
  const [decisions, setDecisions] = useState<ComplianceDecision[]>([])
  const [activeHolds, setActiveHolds] = useState(0)
  const [copiedTransferId, setCopiedTransferId] = useState('')
  const [manualTransferId, setManualTransferId] = useState('')
  const [manualReason, setManualReason] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const holdPages = usePagination(holds)
  const reviewPages = usePagination(reviewQueue)
  const decisionPages = usePagination(decisions, 5)

  async function load() {
    setLoading(true)
    try {
      const [nextHolds, nextReviewQueue, nextDashboard, nextDecisions] =
        await Promise.all([
          api<ComplianceHold[]>('/api/v1/compliance/queue'),
          api<ComplianceReviewTransfer[]>('/api/v1/compliance/review-queue'),
          api<Record<string, number>>('/api/v1/compliance/dashboard'),
          api<ComplianceDecision[]>('/api/v1/compliance/audit-events'),
        ])
      setHolds(nextHolds)
      setReviewQueue(nextReviewQueue)
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

  function useTransferId(transferId: string) {
    setError(null)
    setManualTransferId(transferId)
  }

  function copyTransferId(transferId: string) {
    setError(null)
    void navigator.clipboard.writeText(transferId).then(
      () => setCopiedTransferId(transferId),
      () => setError(new Error(
        'Clipboard access is unavailable. Select and copy the ID manually.',
      )),
    )
  }

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
          <>
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
                  {holdPages.items.map((hold) => (
                    <tr key={hold.holdId}>
                      <td>
                        <strong>{hold.recipientName}</strong>
                        <small className="muted block mono">
                          {hold.transferId.slice(0, 8)}
                        </small>
                      </td>
                      <td>
                        {hold.destinationCountry} / {hold.destinationCurrency}
                      </td>
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
            <Pagination
              state={holdPages.state}
              onPageChange={holdPages.setPage}
            />
          </>
        )}
      </section>

      <section className="card">
        <div className="card-heading">
          <h2>Transfers under review</h2>
          <span className="muted small">
            Eligible for a manual hold · full transfer IDs
          </span>
        </div>

        {loading ? (
          <Loading />
        ) : reviewQueue.length === 0 ? (
          <p className="muted">No transfers are currently under review.</p>
        ) : (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Transfer ID</th>
                    <th>Recipient</th>
                    <th>Destination</th>
                    <th>Amount</th>
                    <th>Submitted</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {reviewPages.items.map((transfer) => (
                    <tr key={transfer.transferId}>
                      <td>
                        <span className="mono small">
                          {transfer.transferId}
                        </span>
                      </td>
                      <td>{transfer.recipientName}</td>
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
                      <td>
                        {transfer.submittedAt
                          ? formatDateTime(transfer.submittedAt)
                          : '—'}
                      </td>
                      <td>
                        <button
                          className="button ghost small"
                          type="button"
                          onClick={() => copyTransferId(transfer.transferId)}
                        >
                          {copiedTransferId === transfer.transferId
                            ? 'Copied'
                            : 'Copy ID'}
                        </button>{' '}
                        <button
                          className="button small"
                          type="button"
                          onClick={() => useTransferId(transfer.transferId)}
                        >
                          Select
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              state={reviewPages.state}
              onPageChange={reviewPages.setPage}
            />
          </>
        )}
      </section>

      <div className="split">
        <section className="card">
          <h2>Place a manual compliance hold</h2>
          <p className="muted small">
            The transfer must currently be UNDER_REVIEW and must not already
            have an active hold. Use Select in the review queue above to fill
            the transfer ID.
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
            <>
              <ul className="plain-list">
                {decisionPages.items.map((decision) => (
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
              <Pagination
                state={decisionPages.state}
                onPageChange={decisionPages.setPage}
              />
            </>
          )}
        </section>
      </div>
    </div>
  )
}
