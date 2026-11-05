import { ApiError } from '../api/client'

export function ErrorAlert({ error }: { error: unknown }) {
  if (!error) {
    return null
  }

  const message = error instanceof Error
    ? error.message
    : 'Something went wrong.'

  const details = error instanceof ApiError
    ? Object.entries(error.validationErrors)
    : []

  return (
    <div className="alert alert-error" role="alert">
      <strong>{message}</strong>
      {details.length > 0 && (
        <ul>
          {details.map(([field, detail]) => (
            <li key={field}>{field}: {detail}</li>
          ))}
        </ul>
      )}
      {error instanceof ApiError && error.requestId && (
        <small>Reference: {error.requestId}</small>
      )}
    </div>
  )
}
