import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'
import type { Transfer } from '../../api/types'
import { ErrorAlert } from '../../components/ErrorAlert'
import { Loading } from '../../components/Loading'
import { Pagination, usePagination } from '../../components/Pagination'
import { StatusBadge } from '../../components/StatusBadge'
import { formatDateTime, formatMoney } from '../../utils/format'

const statusSummary = [
  'DRAFT',
  'UNDER_REVIEW',
  'NEEDS_INFO',
  'COMPLIANCE_HOLD',
  'PROCESSING',
  'UNKNOWN',
  'SUCCEEDED',
] as const

export function CustomerDashboard() {
  const [transfers, setTransfers] = useState<Transfer[]>([])
  const [error, setError] = useState<unknown>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api<Transfer[]>('/api/v1/transfers')
      .then(setTransfers)
      .catch(setError)
      .finally(() => setLoading(false))
  }, [])

  const counts = useMemo(() => {
    return transfers.reduce<Record<string, number>>((accumulator, transfer) => {
      accumulator[transfer.status] = (accumulator[transfer.status] ?? 0) + 1
      return accumulator
    }, {})
  }, [transfers])

  const transferPages = usePagination(transfers)

  if (loading) {
    return <Loading label="Loading your transfers…" />
  }

  return (
    <div className="page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Customer workspace</p>
          <h1>Your transfers</h1>
          <p className="muted">
            Create a beneficiary, request an indicative quote, and submit a
            transfer for operations review.
          </p>
        </div>
        <Link className="button primary" to="/customer/transfers/new">
          Start a transfer
        </Link>
      </div>

      <ErrorAlert error={error} />

      <section className="metric-grid">
        {statusSummary.map((status) => (
          <div className="metric-card" key={status}>
            <StatusBadge status={status} />
            <strong>{counts[status] ?? 0}</strong>
          </div>
        ))}
      </section>

      <section className="card">
        <div className="card-heading">
          <h2>Transfer history</h2>
          <Link className="text-link" to="/customer/beneficiaries">
            Manage beneficiaries
          </Link>
        </div>

        {transfers.length === 0 ? (
          <div className="empty-state">
            <h3>No transfers yet</h3>
            <p className="muted">
              Your drafts and submitted transfers will appear here.
            </p>
            <Link className="button primary" to="/customer/transfers/new">
              Create your first transfer
            </Link>
          </div>
        ) : (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Recipient</th>
                    <th>Destination</th>
                    <th>Send amount</th>
                    <th>Estimated receive</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {transferPages.items.map((transfer) => (
                    <tr key={transfer.id}>
                      <td>
                        <strong>{transfer.recipientName}</strong>
                        <small className="muted block">
                          {transfer.maskedAccountReference}
                        </small>
                      </td>
                      <td>
                        {transfer.destinationCountry} /{' '}
                        {transfer.destinationCurrency}
                      </td>
                      <td>
                        {formatMoney(transfer.sourceAmount, transfer.sourceCurrency)}
                      </td>
                      <td>
                        {formatMoney(transfer.destinationAmount, transfer.destinationCurrency)}
                      </td>
                      <td><StatusBadge status={transfer.status} /></td>
                      <td>{formatDateTime(transfer.createdAt)}</td>
                      <td>
                        <Link
                          className="button small"
                          to={`/customer/transfers/${transfer.id}`}
                        >
                          Open
                        </Link>
                      </td>
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
