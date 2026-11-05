import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../api/client'
import type { TimelineEvent, Transfer } from '../../api/types'
import { ErrorAlert } from '../../components/ErrorAlert'
import { Loading } from '../../components/Loading'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime, formatMoney } from '../../utils/format'

export function CustomerTransferDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [transfer, setTransfer] = useState<Transfer | null>(null)
  const [timeline, setTimeline] = useState<TimelineEvent[]>([])
  const [idempotencyKey, setIdempotencyKey] = useState<string>(
    () => crypto.randomUUID(),
  )
  const [responseMessage, setResponseMessage] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)

  const latestInformationRequest = useMemo(() => {
    return [...timeline]
      .reverse()
      .find((event) => event.eventType === 'INFORMATION_REQUESTED')
  }, [timeline])

  async function load() {
    if (!id) {
      return
    }

    try {
      const [nextTransfer, nextTimeline] = await Promise.all([
        api<Transfer>(`/api/v1/transfers/${id}`),
        api<TimelineEvent[]>(`/api/v1/transfers/${id}/timeline`),
      ])
      setTransfer(nextTransfer)
      setTimeline(nextTimeline)
    } catch (nextError) {
      setError(nextError)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (id) {
      const storageKey = `borderlessos:idempotency:${id}`
      const storedKey = sessionStorage.getItem(storageKey)

      if (storedKey) {
        setIdempotencyKey(storedKey)
      } else {
        const nextKey = crypto.randomUUID()
        sessionStorage.setItem(storageKey, nextKey)
        setIdempotencyKey(nextKey)
      }
    }

    setLoading(true)
    void load()
  }, [id])

  async function submitTransfer() {
    if (!id) {
      return
    }

    setError(null)
    setBusy(true)

    try {
      await api(`/api/v1/transfers/${id}/submit`, {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey },
      })

      await load()
    } catch (nextError) {
      setError(nextError)
    } finally {
      setBusy(false)
    }
  }

  async function respond(event: React.FormEvent) {
    event.preventDefault()

    if (!id) {
      return
    }

    setError(null)
    setBusy(true)

    try {
      await api(`/api/v1/transfers/${id}/respond`, {
        method: 'POST',
        body: { message: responseMessage },
      })
      setResponseMessage('')
      await load()
    } catch (nextError) {
      setError(nextError)
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return <Loading label="Loading transfer…" />
  }

  if (!transfer) {
    return (
      <div className="page narrow">
        <ErrorAlert error={error ?? new Error('Transfer not found.')} />
        <Link className="button" to="/customer">Back to transfers</Link>
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
            Created {formatDateTime(transfer.createdAt)}
          </p>
        </div>
        <div className="heading-actions">
          <StatusBadge status={transfer.status} />
          <Link className="button" to="/customer">Back</Link>
        </div>
      </div>

      <ErrorAlert error={error} />

      {transfer.status === 'DRAFT' && (
        <div className="action-bar">
          <div>
            <strong>Draft not yet submitted.</strong>
            <p className="muted small">
              Submitting consumes the quote and starts operations review.
              The same idempotency key is reused if you retry.
            </p>
          </div>
          <div className="heading-actions">
            <Link
              className="button"
              to={`/customer/transfers/new?draft=${transfer.id}`}
            >
              Edit draft
            </Link>
            <button
              className="button primary"
              onClick={submitTransfer}
              disabled={busy}
            >
              {busy ? 'Submitting…' : 'Submit transfer'}
            </button>
          </div>
        </div>
      )}

      {transfer.status === 'NEEDS_INFO' && (
        <section className="card">
          <h2>Operations needs more information</h2>
          {latestInformationRequest ? (
            <div className="inline-note">
              <strong>Request:</strong>{' '}
              {latestInformationRequest.summary}
            </div>
          ) : (
            <p className="muted">
              An information request is open. Contact operations if you cannot
              see the details.
            </p>
          )}

          <form className="stack" onSubmit={respond}>
            <label>
              Your response
              <textarea
                value={responseMessage}
                onChange={(event) => setResponseMessage(event.target.value)}
                maxLength={1000}
                rows={4}
                required
              />
            </label>
            <button className="button primary" disabled={busy}>
              {busy ? 'Sending…' : 'Send response'}
            </button>
          </form>
        </section>
      )}

      <div className="split">
        <section className="card">
          <h2>Transfer snapshot</h2>
          <dl className="detail-grid">
            <div>
              <dt>Send amount</dt>
              <dd>{formatMoney(transfer.sourceAmount, transfer.sourceCurrency)}</dd>
            </div>
            <div>
              <dt>Estimated receive</dt>
              <dd>
                {formatMoney(
                  transfer.destinationAmount,
                  transfer.destinationCurrency,
                )}
              </dd>
            </div>
            <div>
              <dt>Indicative rate</dt>
              <dd>
                1 {transfer.sourceCurrency} = {transfer.exchangeRate}{' '}
                {transfer.destinationCurrency}
              </dd>
            </div>
            <div>
              <dt>Beneficiary account</dt>
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
          </dl>
          <div className="inline-note">
            Submitted transfers are read-only. Provider outcomes are simulated.
          </div>
        </section>

        <section className="card">
          <h2>Status history</h2>
          <ol className="timeline">
            {timeline.map((event) => (
              <li key={event.id}>
                <span className="timeline-dot" />
                <div>
                  <strong>{event.summary}</strong>
                  <p className="muted small">
                    {event.eventType} · {formatDateTime(event.createdAt)}
                  </p>
                </div>
              </li>
            ))}
          </ol>
        </section>
      </div>
    </div>
  )
}
