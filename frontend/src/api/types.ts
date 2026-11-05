export type Role =
  | 'CUSTOMER'
  | 'OPERATIONS'
  | 'COMPLIANCE'
  | 'ADMIN'
  | 'SYSTEM'

export type TransferStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'NEEDS_INFO'
  | 'COMPLIANCE_HOLD'
  | 'READY_FOR_PROVIDER'
  | 'PROCESSING'
  | 'UNKNOWN'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'REJECTED'

export interface AuthUser {
  email: string
  roles: string[]
}

export interface ApiErrorBody {
  status: number
  code?: string
  message: string
  requestId?: string
  validationErrors?: Record<string, string>
}

export interface Beneficiary {
  id: string
  recipientName: string
  destinationCountry: string
  destinationCurrency: string
  accountReference: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface FxQuote {
  quoteId: string
  sourceCurrency: string
  destinationCurrency: string
  sourceAmount: number
  exchangeRate: number
  destinationAmount: number
  provider: string
  rateDate: string
  createdAt: string
  expiresAt: string
  expired: boolean
  consumed: boolean
}

export interface Transfer {
  id: string
  beneficiaryId: string
  quoteId: string
  quoteSource: string
  recipientName: string
  destinationCountry: string
  destinationCurrency: string
  maskedAccountReference: string
  sourceCurrency: string
  sourceAmount: number
  destinationAmount: number
  exchangeRate: number
  status: TransferStatus
  submittedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface OperationsTransfer extends Transfer {
  customerId: string
}

export interface TimelineEvent {
  id: string
  eventType: string
  previousStatus: TransferStatus | null
  resultingStatus: TransferStatus
  summary: string
  createdAt: string
}

export interface AuditEvent {
  id: string
  transferId: string
  eventType: string
  previousStatus: TransferStatus | null
  resultingStatus: TransferStatus
  actorEmail: string
  actorRole: string
  summary: string
  reason: string | null
  createdAt: string
}

export interface InformationRequest {
  id: string
  transferId: string
  message: string
  requestedByEmail: string
  createdAt: string
  respondedAt: string | null
  respondedByEmail: string | null
  responseMessage: string | null
}

export interface ComplianceHold {
  holdId: string
  transferId: string
  holdStatus: 'ACTIVE' | 'CLEARED'
  transferStatus: TransferStatus
  customerId: string
  recipientName: string
  destinationCountry: string
  destinationCurrency: string
  sourceCurrency: string
  sourceAmount: number
  destinationAmount: number
  triggerType: string
  triggerRuleId: string
  triggerSummary: string
  openedAt: string
  openedByEmail: string
  clearedAt: string | null
  clearedByEmail: string | null
  clearanceReason: string | null
  submittedAt: string | null
}

export interface ComplianceDecision {
  id: string
  transferId: string
  holdId: string | null
  decisionType: string
  actorEmail: string
  actorRole: string
  reason: string | null
  createdAt: string
}

export interface ProviderInquiry {
  id: string
  result: 'INCONCLUSIVE' | 'SUCCESS' | 'FAILURE'
  resolvedOutcome: 'SUCCESS' | 'FAILURE' | null
  actorEmail: string
  createdAt: string
}

export interface ProviderAttempt {
  attemptId: string
  transferId: string
  provider: string
  providerReference: string
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'UNKNOWN'
  scenario: string
  pendingOutcome: string | null
  createdAt: string
  updatedAt: string
  lastInquiryAt: string | null
  inquiries: ProviderInquiry[]
}

export interface AdminUser {
  id: string
  email: string
  displayName: string
  enabled: boolean
  roles: string[]
  createdAt: string
}
