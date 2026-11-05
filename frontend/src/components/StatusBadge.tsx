import type { TransferStatus } from '../api/types'

const labels: Record<TransferStatus, string> = {
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  NEEDS_INFO: 'Needs information',
  COMPLIANCE_HOLD: 'Compliance hold',
  READY_FOR_PROVIDER: 'Ready for provider',
  PROCESSING: 'Processing',
  UNKNOWN: 'Outcome unknown',
  SUCCEEDED: 'Succeeded',
  FAILED: 'Failed',
  REJECTED: 'Rejected',
}

export function StatusBadge({ status }: { status: TransferStatus }) {
  return (
    <span className={`status status-${status.toLowerCase()}`}>
      {labels[status]}
    </span>
  )
}
