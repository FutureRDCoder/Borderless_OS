import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'
import type { OperationsTransfer, TransferStatus } from '../../api/types'
import { ErrorAlert } from '../../components/ErrorAlert'
import { Loading } from '../../components/Loading'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime, formatMoney, humanizeKey } from '../../utils/format'

const filterStatuses: TransferStatus[] = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'NEEDS_INFO',
  'COMPLIANCE_HOLD',
  'READY_FOR_PROVIDER',
  'PROCESSING',
  'UNKNOWN',
  'SUCCEEDED',
  'FAILED',
  'REJECTED',
]

export function OperationsDashboard() {
  const [queue, setQueue] = useState<OperationsTransfer[]>([])
  const [dashboard, setDashboard] = useState<Record<string, number>>({})
  const [filter, setFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)

  async function load() {
    setLoading(true)
    try {
      const queuePath = filter
        ? `/api/v1/operations/queue?status=${filter}`
        : '/api/v1/operations/queue'

      const [nextQueue, nextDashboard] = await Promise.all([
        api<OperationsTransfer[]>(queuePath),
        api<Record<string, number>>('/api/v1/operations/dashboard'),
      ])
      setQueue(nextQueue)
      setDashboard(nextDashboard)
    } catch (nextError) {
      setError(nextError)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [filter])

  return (
    <div className="page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Operations workspace</p>
          <h1>Review queue</h1>
          <p className="muted">
            Review submitted transfers, request missing information, reject
            invalid requests, and trigger deterministic mock-provider attempts.
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

      <section className="card">
        <div className="card-heading">
          <h2>Transfers</h2>
          <label className="inline-label">
            Status
            <select
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            >
              <option value="">Open queue</option>
              {filterStatuses.map((status) => (
                <option key={status} value={status}>{status}</option>
              ))}
            </select>
          </label>
        </div>

        {loading ? (
          <Loading />
        ) : queue.length === 0 ? (
          <p className="muted">No transfers match this view.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Transfer</th>
                  <th>Customer</th>
                  <th>Destination</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Updated</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {queue.map((transfer) => (
                  <tr key={transfer.id}>
                    <td>
                      <strong>{transfer.recipientName}</strong>
                      <small className="muted block mono">
                        {transfer.id.slice(0, 8)}
                      </small>
                    </td>
                    <td className="mono small">{transfer.customerId.slice(0, 8)}</td>
                    <td>{transfer.destinationCountry} / {transfer.destinationCurrency}</td>
                    <td>{formatMoney(transfer.sourceAmount, transfer.sourceCurrency)}</td>
                    <td><StatusBadge status={transfer.status} /></td>
                    <td>{formatDateTime(transfer.updatedAt)}</td>
                    <td>
                      <Link
                        className="button small"
                        to={`/operations/transfers/${transfer.id}`}
                      >
                        Review
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
