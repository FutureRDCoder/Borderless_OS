import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/client'
import type { Beneficiary } from '../../api/types'
import { ErrorAlert } from '../../components/ErrorAlert'
import { Loading } from '../../components/Loading'
import { Pagination, usePagination } from '../../components/Pagination'
import { formatDateTime } from '../../utils/format'

const destinations = [
  { country: 'US', currency: 'USD', label: 'United States — USD' },
  { country: 'GB', currency: 'GBP', label: 'United Kingdom — GBP' },
  { country: 'DE', currency: 'EUR', label: 'Germany — EUR' },
  { country: 'FR', currency: 'EUR', label: 'France — EUR' },
  { country: 'IT', currency: 'EUR', label: 'Italy — EUR' },
  { country: 'ES', currency: 'EUR', label: 'Spain — EUR' },
  { country: 'NL', currency: 'EUR', label: 'Netherlands — EUR' },
  { country: 'AU', currency: 'AUD', label: 'Australia — AUD' },
  { country: 'NZ', currency: 'NZD', label: 'New Zealand — NZD' },
  { country: 'JP', currency: 'JPY', label: 'Japan — JPY' },
  { country: 'IN', currency: 'INR', label: 'India — INR' },
]

const blankForm = {
  recipientName: '',
  destination: 'US',
  accountReference: '',
}

export function BeneficiariesPage() {
  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([])
  const [form, setForm] = useState(blankForm)
  const [error, setError] = useState<unknown>(null)
  const [formError, setFormError] = useState<unknown>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const beneficiaryPages = usePagination(beneficiaries, 5)

  async function load() {
    setLoading(true)
    try {
      setBeneficiaries(await api<Beneficiary[]>('/api/v1/beneficiaries'))
    } catch (nextError) {
      setError(nextError)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(null)
    setBusy(true)

    const destination = destinations.find(
      (item) => item.country === form.destination,
    )!

    try {
      await api('/api/v1/beneficiaries', {
        method: 'POST',
        body: {
          recipientName: form.recipientName,
          destinationCountry: destination.country,
          destinationCurrency: destination.currency,
          accountReference: form.accountReference,
        },
      })
      setForm(blankForm)
      await load()
    } catch (nextError) {
      setFormError(nextError)
    } finally {
      setBusy(false)
    }
  }

  async function deactivate(beneficiary: Beneficiary) {
    if (!window.confirm(
      `Deactivate ${beneficiary.recipientName}? Existing transfers keep ` +
      'their original submission snapshot.',
    )) {
      return
    }

    try {
      await api(`/api/v1/beneficiaries/${beneficiary.id}`, {
        method: 'DELETE',
      })
      await load()
    } catch (nextError) {
      setError(nextError)
    }
  }

  return (
    <div className="page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Customer workspace</p>
          <h1>Beneficiaries</h1>
          <p className="muted">
            Beneficiaries can be soft-deactivated. Historical transfers keep
            an immutable submission-time snapshot.
          </p>
        </div>
      </div>

      <ErrorAlert error={error} />

      <div className="split">
        <section className="card">
          <div className="card-heading">
            <h2>Saved beneficiaries</h2>
            <span className="muted small">{beneficiaries.length} active</span>
          </div>

          {loading ? (
            <Loading />
          ) : beneficiaries.length === 0 ? (
            <p className="muted">No active beneficiaries yet.</p>
          ) : (
            <>
              <div className="stack">
                {beneficiaryPages.items.map((beneficiary) => (
                  <article className="list-row" key={beneficiary.id}>
                    <div>
                      <strong>{beneficiary.recipientName}</strong>
                      <p className="muted small">
                        {beneficiary.destinationCountry} /{' '}
                        {beneficiary.destinationCurrency} ·{' '}
                        {beneficiary.accountReference}
                      </p>
                      <p className="muted small">
                        Added {formatDateTime(beneficiary.createdAt)}
                      </p>
                    </div>
                    <button
                      className="button small danger"
                      onClick={() => deactivate(beneficiary)}
                    >
                      Deactivate
                    </button>
                  </article>
                ))}
              </div>
              <Pagination
                state={beneficiaryPages.state}
                onPageChange={beneficiaryPages.setPage}
              />
            </>
          )}
        </section>

        <section className="card">
          <h2>Add beneficiary</h2>
          <form className="stack" onSubmit={handleSubmit}>
            <label>
              Recipient name
              <input
                value={form.recipientName}
                onChange={(event) => setForm({
                  ...form,
                  recipientName: event.target.value,
                })}
                maxLength={120}
                required
              />
            </label>

            <label>
              Destination
              <select
                value={form.destination}
                onChange={(event) => setForm({
                  ...form,
                  destination: event.target.value,
                })}
              >
                {destinations.map((destination) => (
                  <option
                    key={destination.country}
                    value={destination.country}
                  >
                    {destination.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Account reference
              <input
                value={form.accountReference}
                onChange={(event) => setForm({
                  ...form,
                  accountReference: event.target.value,
                })}
                maxLength={64}
                placeholder="Synthetic account reference"
                required
              />
            </label>

            <ErrorAlert error={formError} />

            <button className="button primary" disabled={busy}>
              {busy ? 'Saving…' : 'Save beneficiary'}
            </button>
          </form>
        </section>
      </div>
    </div>
  )
}
