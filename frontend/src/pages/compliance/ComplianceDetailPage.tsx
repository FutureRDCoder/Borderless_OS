import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import type {
  ComplianceDecision,
  ComplianceHold,
} from '../../api/types'
import { ErrorAlert } from '../../components/ErrorAlert'
import { Loading } from '../../components/Loading'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime, formatMoney } from '../../utils/format'

export function ComplianceDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [hold, setHold] = useState<ComplianceHold | null>(null)
  const [decisions, setDecisions] = useState<ComplianceDecision[]>([])
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [notice, setNotice] = useState('')

  async function loadDecisions(transferId: string) {
    setDecisions(
      await api<ComplianceDecision[]>(
        `/api/v1/compliance/transfers/${transferId}/decisions`,
      ),
    )
  }

  useEffect(() => {
    if (!id) {
      return
    }

    setLoading(true)
    Promise.all([
      api<ComplianceHold>(`/api/v1/compliance/transfers/${id}`),
      api<ComplianceDecision[]>(
        `/api/v1/compliance/transfers/${id}/decisions`,
      ),
    ])
      .then(([nextHold, nextDecisions]) => {
        setHold(nextHold)
        setDecisions(nextDecisions)
      })
      .catch((nextError) => {
        if (nextError instanceof ApiError && nextError.status === 404) {
          setError(new Error(
            'No active hold was found. It may already have been cleared.',
          ))
        } else {
          setError(nextError)
        }
      })
      .finally(() => setLoading(false))
  }, [id])

  async function decide(action: 'maintain-hold' | 'clear-hold') {
    if (!id) {
      return
    }

    setError(null)
    setNotice('')
    setBusy(true)

    try {
      const updated = await api<ComplianceHold>(
        `/api/v1/compliance/transfers/${id}/${action}`,
        {
          method: 'POST',
          body: { reason },
        },
      )
      setHold(updated)
      setReason('')
      setNotice(
        action === 'clear-hold'
          ? 'Hold cleared. Transfer returned to operations review.'
          : 'Hold maintained and decision recorded.',
      )
      await loadDecisions(id)
    } catch (nextError) {
      setError(nextError)
    } finally {
      setBusy(false)
    }
  }

  async function handleMaintain(event: FormEvent) {
    event.preventDefault()
    await decide('maintain-hold')
  }

  if (loading) {
    return <Loading label="Loading compliance hold…" />
  }

  if (!hold) {
    return (
      <div className="page narrow">
        <ErrorAlert error={error ?? new Error('Hold not found.')} />
        <Link className="button" to="/compliance">Back to queue</Link>
      </div>
    )
  }

  const canDecide = hold.holdStatus === 'ACTIVE'
    && hold.transferStatus === 'COMPLIANCE_HOLD'

  return (
    <div className="page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Compliance hold {hold.holdId.slice(0, 8)}</p>
          <h1>{hold.recipientName}</h1>
          <p className="muted">
            {hold.destinationCountry} / {hold.destinationCurrency} ·{' '}
            {formatMoney(hold.sourceAmount, hold.sourceCurrency)} →{' '}
            {formatMoney(hold.destinationAmount, hold.destinationCurrency)}
          </p>
        </div>
        <div className="heading-actions">
          <StatusBadge status={hold.transferStatus} />
          <Link className="button" to="/compliance">Back</Link>
        </div>
      </div>

      <ErrorAlert error={error} />
      {notice && <div className="alert alert-success">{notice}</div>}

      <div className="split">
        <section className="card">
          <h2>Hold details</h2>
          <dl className="detail-grid">
            <div>
              <dt>Hold status</dt>
              <dd>{hold.holdStatus}</dd>
            </div>
            <div>
              <dt>Trigger type</dt>
              <dd>{hold.triggerType}</dd>
            </div>
            <div>
              <dt>Rule</dt>
              <dd>{hold.triggerRuleId}</dd>
            </div>
            <div>
              <dt>Opened</dt>
              <dd>{formatDateTime(hold.openedAt)}</dd>
            </div>
            <div>
              <dt>Opened by</dt>
              <dd>{hold.openedByEmail}</dd>
            </div>
            <div>
              <dt>Transfer</dt>
              <dd className="mono">{hold.transferId}</dd>
            </div>
          </dl>

          <div className="inline-note warning">
            <strong>Illustrative trigger:</strong> {hold.triggerSummary}
          </div>

          {hold.clearedAt && (
            <div className="inline-note">
              <strong>Cleared {formatDateTime(hold.clearedAt)}</strong> by{' '}
              {hold.clearedByEmail}. Reason: {hold.clearanceReason}
            </div>
          )}
        </section>

        <section className="card">
          <h2>Compliance decision</h2>
          {!canDecide ? (
            <p className="muted">
              This hold is no longer active, so no further decision can be
              recorded.
            </p>
          ) : (
            <form className="stack" onSubmit={handleMaintain}>
              <label>
                Mandatory reason
                <textarea
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  maxLength={1000}
                  rows={4}
                  placeholder="Record the basis for maintaining or clearing the hold."
                  required
                />
              </label>
              <div className="heading-actions">
                <button className="button" disabled={busy}>
                  Maintain hold
                </button>
                <button
                  type="button"
                  className="button primary"
                  disabled={busy}
                  onClick={() => decide('clear-hold')}
                >
                  Clear hold
                </button>
              </div>
            </form>
          )}
        </section>
      </div>

      <section className="card">
        <h2>Decision history</h2>
        {decisions.length === 0 ? (
          <p className="muted">No decisions recorded.</p>
        ) : (
          <ol className="timeline">
            {decisions.map((decision) => (
              <li key={decision.id}>
                <span className="timeline-dot" />
                <div>
                  <strong>{decision.decisionType}</strong>
                  <p className="muted small">
                    {decision.actorEmail} · {decision.actorRole} ·{' '}
                    {formatDateTime(decision.createdAt)}
                  </p>
                  {decision.reason && (
                    <p className="small">Reason: {decision.reason}</p>
                  )}
                </div>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  )
}
