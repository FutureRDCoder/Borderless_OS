import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../../api/client'
import type { Beneficiary, FxQuote, Transfer } from '../../api/types'
import { ErrorAlert } from '../../components/ErrorAlert'
import { Loading } from '../../components/Loading'
import { formatDateTime, formatMoney } from '../../utils/format'

const supportedCurrencies = ['INR', 'USD', 'EUR', 'GBP', 'AUD', 'NZD', 'JPY']

export function NewTransferPage() {
  const [searchParams] = useSearchParams()
  const draftId = searchParams.get('draft')
  const navigate = useNavigate()

  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([])
  const [beneficiaryId, setBeneficiaryId] = useState('')
  const [sourceCurrency, setSourceCurrency] = useState('INR')
  const [sourceAmount, setSourceAmount] = useState('1000')
  const [quote, setQuote] = useState<FxQuote | null>(null)
  const [draft, setDraft] = useState<Transfer | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)

  const selectedBeneficiary = useMemo(
    () => beneficiaries.find((item) => item.id === beneficiaryId),
    [beneficiaries, beneficiaryId],
  )

  const sourceOptions = useMemo(() => {
    if (selectedBeneficiary?.destinationCurrency === 'INR') {
      return supportedCurrencies.filter((currency) => currency !== 'INR')
    }

    return ['INR']
  }, [selectedBeneficiary])

  useEffect(() => {
    async function load() {
      try {
        const [beneficiaryList, existingDraft] = await Promise.all([
          api<Beneficiary[]>('/api/v1/beneficiaries'),
          draftId
            ? api<Transfer>(`/api/v1/transfers/${draftId}`)
            : Promise.resolve(null),
        ])

        setBeneficiaries(beneficiaryList)

        if (existingDraft) {
          if (existingDraft.status !== 'DRAFT') {
            throw new Error(
              'Only a draft transfer can be edited.',
            )
          }
          setDraft(existingDraft)
          setBeneficiaryId(existingDraft.beneficiaryId)
          setSourceCurrency(existingDraft.sourceCurrency)
          setSourceAmount(String(existingDraft.sourceAmount))
        } else if (beneficiaryList.length === 1) {
          setBeneficiaryId(beneficiaryList[0].id)
        }
      } catch (nextError) {
        setError(nextError)
      } finally {
        setLoading(false)
      }
    }

    void load()
  }, [draftId])

  useEffect(() => {
    if (sourceOptions.length > 0 && !sourceOptions.includes(sourceCurrency)) {
      setSourceCurrency(sourceOptions[0])
    }
  }, [sourceOptions, sourceCurrency])

  function changeBeneficiary(nextId: string) {
    setBeneficiaryId(nextId)
    setQuote(null)
  }

  async function requestQuote(event: FormEvent) {
    event.preventDefault()
    setError(null)

    if (!selectedBeneficiary) {
      setError(new Error('Select a beneficiary first.'))
      return
    }

    setBusy(true)

    try {
      const nextQuote = await api<FxQuote>('/api/v1/fx/quotes', {
        method: 'POST',
        body: {
          sourceCurrency,
          destinationCurrency: selectedBeneficiary.destinationCurrency,
          sourceAmount: Number(sourceAmount),
        },
      })
      setQuote(nextQuote)
    } catch (nextError) {
      setError(nextError)
    } finally {
      setBusy(false)
    }
  }

  async function saveDraft() {
    if (!quote) {
      return
    }

    setError(null)
    setBusy(true)

    try {
      const path = draftId
        ? `/api/v1/transfers/${draftId}`
        : '/api/v1/transfers'

      const saved = await api<Transfer>(path, {
        method: draftId ? 'PATCH' : 'POST',
        body: {
          beneficiaryId,
          quoteId: quote.quoteId,
        },
      })

      navigate(`/customer/transfers/${saved.id}`)
    } catch (nextError) {
      setError(nextError)
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return <Loading label="Preparing transfer workspace…" />
  }

  return (
    <div className="page narrow">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Customer workspace</p>
          <h1>{draft ? 'Edit draft transfer' : 'New transfer'}</h1>
          <p className="muted">
            Quotes are indicative, valid for five minutes, and can be used by
            only one successfully submitted transfer.
          </p>
        </div>
      </div>

      <ErrorAlert error={error} />

      {beneficiaries.length === 0 ? (
        <div className="card empty-state">
          <h2>Add a beneficiary first</h2>
          <p className="muted">
            A transfer draft needs an active beneficiary and a valid FX quote.
          </p>
          <button
            className="button primary"
            onClick={() => navigate('/customer/beneficiaries')}
          >
            Manage beneficiaries
          </button>
        </div>
      ) : (
        <div className="split">
          <section className="card">
            <h2>1. Transfer details</h2>
            <form className="stack" onSubmit={requestQuote}>
              <label>
                Beneficiary
                <select
                  value={beneficiaryId}
                  onChange={(event) => changeBeneficiary(event.target.value)}
                  required
                >
                  <option value="">Select a beneficiary</option>
                  {beneficiaries.map((beneficiary) => (
                    <option key={beneficiary.id} value={beneficiary.id}>
                      {beneficiary.recipientName} —{' '}
                      {beneficiary.destinationCountry} /{' '}
                      {beneficiary.destinationCurrency}
                    </option>
                  ))}
                </select>
              </label>

              <div className="field-row">
                <label>
                  You send
                  <select
                    value={sourceCurrency}
                    onChange={(event) => {
                      setSourceCurrency(event.target.value)
                      setQuote(null)
                    }}
                  >
                    {sourceOptions.map((currency) => (
                      <option key={currency} value={currency}>
                        {currency}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Amount
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={sourceAmount}
                    onChange={(event) => {
                      setSourceAmount(event.target.value)
                      setQuote(null)
                    }}
                    required
                  />
                </label>
              </div>

              <div className="inline-note">
                Sending to {selectedBeneficiary?.destinationCountry ?? '—'} in{' '}
                {selectedBeneficiary?.destinationCurrency ?? '—'}.
              </div>

              <button className="button primary" disabled={busy}>
                {busy ? 'Requesting quote…' : 'Request indicative quote'}
              </button>
            </form>
          </section>

          <section className="card">
            <h2>2. Review quote & save draft</h2>

            {!quote ? (
              <p className="muted">
                Request a quote to see the indicative exchange rate and
                estimated destination amount.
              </p>
            ) : (
              <div className="stack">
                <dl className="detail-grid">
                  <div>
                    <dt>You send</dt>
                    <dd>
                      {formatMoney(
                        quote.sourceAmount,
                        quote.sourceCurrency,
                      )}
                    </dd>
                  </div>
                  <div>
                    <dt>Indicative rate</dt>
                    <dd>1 {quote.sourceCurrency} = {quote.exchangeRate} {quote.destinationCurrency}</dd>
                  </div>
                  <div>
                    <dt>Estimated receive</dt>
                    <dd>
                      {formatMoney(
                        quote.destinationAmount,
                        quote.destinationCurrency,
                      )}
                    </dd>
                  </div>
                  <div>
                    <dt>Rate source</dt>
                    <dd>{quote.provider.toUpperCase()} (indicative)</dd>
                  </div>
                  <div>
                    <dt>Quote expires</dt>
                    <dd>{formatDateTime(quote.expiresAt)}</dd>
                  </div>
                  <div>
                    <dt>Quote ID</dt>
                    <dd className="mono">{quote.quoteId}</dd>
                  </div>
                </dl>

                <div className="inline-note">
                  The quote is consumed only when you submit the transfer.
                  Saving a draft does not consume it.
                </div>

                <button
                  className="button primary"
                  onClick={saveDraft}
                  disabled={busy}
                >
                  {busy
                    ? 'Saving…'
                    : draft
                      ? 'Update draft'
                      : 'Save draft and continue'}
                </button>
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  )
}
