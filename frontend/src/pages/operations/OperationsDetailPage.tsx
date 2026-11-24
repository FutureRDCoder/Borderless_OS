import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import type {
  AuditEvent,
  InformationRequest,
  OperationsTransfer,
  ProviderAttempt,
} from '../../api/types'
import { ErrorAlert } from '../../components/ErrorAlert'
import { Loading } from '../../components/Loading'
import { Pagination, usePagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime, formatMoney } from '../../utils/format'

export function OperationsDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [transfer, setTransfer] = useState<OperationsTransfer | null>(null)
  const [auditEvents, setAuditEvents] = useState<AuditEvent[]>([])
  const [informationRequests, setInformationRequests] = useState<
    InformationRequest[]
  >([])
  const [providerAttempt, setProviderAttempt] =
    useState<ProviderAttempt | null>(null)
  const [informationMessage, setInformationMessage] = useState('')
  const [rejectionReason, setRejectionReason] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const inquiryPages = usePagination(providerAttempt?.inquiries ?? [], 5)
  const informationPages = usePagination(informationRequests, 5)
  const auditPages = usePagination(auditEvents, 5)

  async function load() {
    if (!id) {
      return
    }

    try {
      const [nextTransfer, nextAudit, nextInfo] = await Promise.all([
        api<OperationsTransfer>(`/api/v1/operations/transfers/${id}`),
        api<AuditEvent[]>(
          `/api/v1/operations/audit-events?transferId=${id}`,
        ),
        api<InformationRequest[]>(
          `/api/v1/operations/transfers/${id}/information-requests`,
        ),
      ])

      let nextAttempt: ProviderAttempt | null = null

      try {
        nextAttempt = await api<ProviderAttempt>(
          `/api/v1/operations/transfers/${id}/provider-attempt`,
        )
      } catch (attemptError) {
        if (!(attemptError instanceof ApiError)
            || attemptError.status !== 404) {
          throw attemptError
        }
      }

      setTransfer(nextTransfer)
      setAuditEvents(nextAudit)
      setInformationRequests(nextInfo)
      setProviderAttempt(nextAttempt)
    } catch (nextError) {
      setError(nextError)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setLoading(true)
    void load()
  }, [id])

  async function perform(path: string, body?: unknown) {
    if (!id) {
      return
    }

    setError(null)
    setBusy(true)

    try {
      await api(`/api/v1/operations/transfers/${id}/${path}`, {
        method: 'POST',
        body,
      })
      setInformationMessage('')
      setRejectionReason('')
      await load()
    } catch (nextError) {
      setError(nextError)
    } finally {
      setBusy(false)
    }
  }

  async function requestInformation(event: FormEvent) {
    event.preventDefault()
    await perform('request-info', { message: informationMessage })
  }

  async function rejectTransfer(event: FormEvent) {
    event.preventDefault()
    await perform('reject', { reason: rejectionReason })
  }

  if (loading) {
    return <Loading label="Loading review workspace…" />
  }

  if (!transfer) {
    return (
      <div className="page narrow">
        <ErrorAlert error={error ?? new Error('Transfer not found.')} />
        <Link className="button" to="/operations">Back to queue</Link>
      </div>
    )
  }

  return (
    <div className="page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Transfer {transfer.id.slice(0, 8)}</p>
          <h1>{transfer.recipientName}</h1>
          <p className="muted">
            {transfer.destinationCountry} / {transfer.destinationCurrency} ·{' '}
            {formatMoney(transfer.sourceAmount, transfer.sourceCurrency)} →{' '}
            {formatMoney(
              transfer.destinationAmount,
              transfer.destinationCurrency,
            )}
          </p>
        </div>
        <div className="heading-actions">
          <StatusBadge status={transfer.status} />
          <Link className="button" to="/operations">Back</Link>
        </div>
      </div>

      <ErrorAlert error={error} />

      {transfer.status === 'COMPLIANCE_HOLD' && (
        <div className="inline-note warning">
          This transfer is on compliance hold. Operations cannot dispatch or
          mark it ready. Only an authorised Compliance user can clear the hold.
        </div>
      )}

      {transfer.status === 'UNDER_REVIEW' && (
        <section className="card">
          <h2>Review actions</h2>
          <div className="split">
            <form className="stack" onSubmit={requestInformation}>
              <label>
                Request missing information
                <textarea
                  value={informationMessage}
                  onChange={(event) =>
                    setInformationMessage(event.target.value)}
                  maxLength={1000}
                  rows={3}
                  placeholder="Tell the customer what is required and why."
                  required
                />
              </label>
              <button className="button" disabled={busy}>
                Send information request
              </button>
            </form>

            <div className="stack">
              <button
                className="button primary"
                disabled={busy}
                onClick={() => perform('mark-ready')}
              >
                Mark ready for provider
              </button>

              <form className="stack" onSubmit={rejectTransfer}>
                <label>
                  Rejection reason
                  <textarea
                    value={rejectionReason}
                    onChange={(event) =>
                      setRejectionReason(event.target.value)}
                    maxLength={1000}
                    rows={3}
                    placeholder="Mandatory operational rejection reason."
                    required
                  />
                </label>
                <button className="button danger" disabled={busy}>
                  Reject transfer
                </button>
              </form>
            </div>
          </div>
        </section>
      )}

      {transfer.status === 'READY_FOR_PROVIDER' && (
        <section className="card">
          <h2>Mock provider dispatch</h2>
          <p className="muted">
            Dispatch is simulated and deterministic by destination corridor.
            It cannot be triggered when a compliance hold is active.
          </p>
          <button
            className="button primary"
            disabled={busy}
            onClick={() => perform('dispatch')}
          >
            Start mock provider attempt
          </button>
        </section>
      )}

      {transfer.status === 'UNKNOWN' && (
        <section className="card">
          <h2>Uncertain provider outcome</h2>
          <p className="muted">
            UNKNOWN is not a failure. Perform a controlled status inquiry; no
            blind retry or new dispatch is allowed.
          </p>
          <button
            className="button primary"
            disabled={busy}
            onClick={() => perform('provider-status-inquiry')}
          >
            Run status inquiry
          </button>
        </section>
      )}

      {providerAttempt && (
        <section className="card">
          <h2>Provider attempt</h2>
          <dl className="detail-grid">
            <div>
              <dt>Provider</dt>
              <dd>{providerAttempt.provider}</dd>
            </div>
            <div>
              <dt>Reference</dt>
              <dd className="mono">{providerAttempt.providerReference}</dd>
            </div>
            <div>
              <dt>Attempt status</dt>
              <dd>{providerAttempt.status}</dd>
            </div>
            <div>
              <dt>Demo scenario</dt>
              <dd>{providerAttempt.scenario}</dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{formatDateTime(providerAttempt.createdAt)}</dd>
            </div>
            <div>
              <dt>Last inquiry</dt>
              <dd>{formatDateTime(providerAttempt.lastInquiryAt)}</dd>
            </div>
          </dl>

          <h3>Inquiry history</h3>
          {providerAttempt.inquiries.length === 0 ? (
            <p className="muted small">No status inquiries yet.</p>
          ) : (
            <>
              <ul className="plain-list">
                {inquiryPages.items.map((inquiry) => (
                  <li key={inquiry.id}>
                    <strong>{inquiry.result}</strong>
                    {inquiry.resolvedOutcome
                      ? ` → ${inquiry.resolvedOutcome}`
                      : ''}
                    <span className="muted small">
                      {' '}· {formatDateTime(inquiry.createdAt)}
                    </span>
                  </li>
                ))}
              </ul>
              <Pagination
                state={inquiryPages.state}
                onPageChange={inquiryPages.setPage}
              />
            </>
          )}
        </section>
      )}

      <div className="split">
        <section className="card">
          <h2>Transfer details</h2>
          <dl className="detail-grid">
            <div>
              <dt>Customer</dt>
              <dd className="mono">{transfer.customerId}</dd>
            </div>
            <div>
              <dt>Beneficiary</dt>
              <dd className="mono">{transfer.beneficiaryId}</dd>
            </div>
            <div>
              <dt>Account</dt>
              <dd>{transfer.maskedAccountReference}</dd>
            </div>
            <div>
              <dt>Quote source</dt>
              <dd>{transfer.quoteSource}</dd>
            </div>
            <div>
              <dt>Submitted</dt>
              <dd>{formatDateTime(transfer.submittedAt)}</dd>
            </div>
            <div>
              <dt>Indicative rate</dt>
              <dd>
                1 {transfer.sourceCurrency} = {transfer.exchangeRate}{' '}
                {transfer.destinationCurrency}
              </dd>
            </div>
          </dl>

          <h3>Information requests</h3>
          {informationRequests.length === 0 ? (
            <p className="muted small">No information requests.</p>
          ) : (
            <>
              <ul className="plain-list">
                {informationPages.items.map((request) => (
                  <li key={request.id}>
                    <strong>{request.message}</strong>
                    <p className="muted small">
                      Requested {formatDateTime(request.createdAt)}
                    </p>
                    {request.responseMessage && (
                      <p className="small">
                        Customer: {request.responseMessage}
                      </p>
                    )}
                  </li>
                ))}
              </ul>
              <Pagination
                state={informationPages.state}
                onPageChange={informationPages.setPage}
              />
            </>
          )}
        </section>

        <section className="card">
          <h2>Audit history</h2>
          <ol className="timeline">
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
        </section>
      </div>
    </div>
  )
}
