# BorderlessOS — Business Rules 06
## Mock Provider Processing, Idempotency, and Outcome Reconciliation

**Document status:** Approved  
**Project:** BorderlessOS — Cross-Border Remittance & Compliance Operations Platform  
**Scope:** Five-day educational prototype  
**Version:** 1.0

---

## 1. Purpose

This document defines BR-06 for BorderlessOS: the mock-provider attempt lifecycle, idempotent attempt creation, timeout handling, controlled status inquiry, outcome reconciliation, and associated audit and concurrency requirements.

BorderlessOS is an educational prototype only. It is not a licensed financial product or certified compliance system. It must not move, custody, disburse, or settle real funds. All provider outcomes and transfer processing described here are simulated.

## 2. Approved design decisions

The following decisions were explicitly approved:

- **Mock outcome selection:** deterministic scenarios.
- **Timeout handling:** transition to `UNKNOWN` and require an explicit status inquiry.
- **Reconciliation authority:** Operations may initiate a status inquiry; only a confirmed mock-provider result resolves the outcome.
- **Retry policy:** no automatic retries and no new attempt while the outcome is uncertain. The MVP does not support a new attempt after confirmed failure.
- **Provider implementation:** local mock-provider adapter only; no real provider integration.

## 3. Business objective

Simulate the provider-processing lifecycle safely and reproducibly. Prevent duplicate logical attempts, preserve uncertain outcomes rather than guessing, and provide a controlled way to inquire about the existing attempt without triggering another attempt.

## 4. Roles and permissions

| Role | BR-06 permissions |
|---|---|
| **CUSTOMER** | May view their own permitted transfer status. Cannot dispatch attempts, initiate Operations inquiries, or set provider outcomes. |
| **OPERATIONS** | May view permitted provider-attempt information and initiate an allowed status inquiry for a transfer in `UNKNOWN`. Cannot arbitrarily declare success or failure. |
| **COMPLIANCE** | Retains hold decision authority under BR-04. Clearing a hold returns the transfer to `UNDER_REVIEW`; it does not initiate provider dispatch. |
| **ADMIN** | Has only explicitly granted application-administration permissions. Admin status alone does not grant provider-outcome override or arbitrary transfer-state authority. |
| **SYSTEM** | Controls eligible dispatch, creates the logical mock-provider attempt, records outcomes, and applies valid state transitions. |

All permissions must be enforced in the backend. Frontend controls alone are not authorisation.

## 5. Business rules

### BR-06.1 — Deterministic mock provider

- Implement a local mock-provider adapter behind a provider interface.
- Simulated outcomes must be deterministic and configured by test scenarios or explicit rules.
- Support confirmed success, confirmed failure, timeout/ambiguous outcome, and inconclusive status inquiry.
- Repeating the same logical attempt must not create a different outcome merely because it was called again.
- Clearly label all results as simulated. Never call a real payment execution API.

### BR-06.2 — Dispatch eligibility

- Only transfers in `READY_FOR_PROVIDER` may enter the provider-attempt workflow.
- Before creating an attempt, the backend must revalidate the transfer state and confirm that no Compliance hold is active.
- Customers cannot trigger arbitrary provider attempts.
- Only the system-controlled workflow can initiate the mock-provider operation after all preconditions pass.
- A transfer already in `PROCESSING`, `UNKNOWN`, or a terminal state cannot be dispatched again through the normal initiation operation.

### BR-06.3 — Idempotent attempt creation

- Each logical attempt must have a unique durable attempt identifier and idempotency key.
- Duplicate requests for the same logical operation must return the original attempt or its known outcome, not create a second attempt.
- Reusing a key with materially different request data must return a conflict.
- Persist the attempt record and relevant state change safely, with database uniqueness constraints and concurrency controls.
- Provider calls should not be made inside a long-running database transaction.

### BR-06.4 — Timeout and uncertain outcomes

- A timeout, lost response, or inconclusive provider response must not be treated as confirmed failure.
- If the outcome cannot be established, the transfer transitions to `UNKNOWN`.
- Preserve the original attempt identifier and provider reference for later inquiry.
- The system must not automatically create a replacement attempt after a timeout.
- The transfer remains `UNKNOWN` until a controlled inquiry provides a confirmed result or remains inconclusive.

### BR-06.5 — Explicit status inquiry

- Operations may initiate a status inquiry for a transfer in `UNKNOWN`.
- The inquiry checks the status of the **existing attempt**; it does not create a new attempt.
- A confirmed success resolves the transfer to `SUCCEEDED`.
- A confirmed failure resolves the transfer to `FAILED`.
- An inconclusive result leaves the transfer in `UNKNOWN`.
- Only the mock-provider adapter's confirmed simulated result may resolve the transfer through this inquiry path.
- Operations cannot manually declare success or failure merely because an inquiry was inconclusive.

### BR-06.6 — No retry policy in the MVP

- Automatic retries are disabled.
- No new provider attempt may be initiated while the transfer is in `UNKNOWN`.
- A timeout must never trigger a blind retry.
- The MVP does not provide a retry-after-failure feature. A future version may introduce a separate, explicitly authorised policy after a confirmed failure, but that is out of scope for BR-06.
- Duplicate client or system requests must be handled through idempotency, not retries.

### BR-06.7 — Durable attempt and outcome history

Each attempt, inquiry, and resolution must be recorded as a durable event. At minimum, record:

- transfer ID and attempt ID;
- idempotency key or safe reference;
- event type, such as attempt created, provider outcome received, inquiry requested, inquiry completed, or reconciliation result recorded;
- actor or system identity;
- server-generated timestamps;
- previous and resulting transfer states;
- simulated provider result and safe provider reference;
- error classification or uncertainty reason, where applicable.

History must be append-only from the application's business perspective. Do not overwrite the original timeout event when a later inquiry resolves the outcome.

### BR-06.8 — Backend authority and role restrictions

- The backend owns all state transitions.
- Customers can view their own permitted transfer status but cannot set provider outcomes.
- Operations can view permitted attempt details and initiate allowed status inquiries.
- Operations cannot arbitrarily mark a transfer successful or failed.
- Compliance retains its BR-04 authority over holds; clearing a hold returns the transfer to `UNDER_REVIEW` and does not initiate dispatch.
- Admin does not have blanket permission to override provider outcomes or transfer states.

### BR-06.9 — Atomicity and concurrency

- Prevent concurrent dispatch requests from creating multiple logical attempts.
- Use database constraints, transactions, and an appropriate concurrency strategy such as optimistic locking or row-level locking.
- Persist the attempt record, transfer transition, and required audit event atomically where they share the database.
- Treat a provider interaction as a separate step from the database transaction. If the application crashes after creating an attempt but before recording a response, recover by querying the same attempt rather than creating another one.
- If audit persistence fails, the associated state transition must not commit.

### BR-06.10 — Safe failure and simulation boundary

- A database or audit failure must not leave an untracked committed state transition.
- A provider timeout must not be converted into a success or failure without confirmation.
- An inconclusive inquiry must not release a transfer from `UNKNOWN`.
- Provider simulation data must be synthetic.
- The prototype must not move, custody, disburse, or settle real funds, and must not claim production payment-processing capability.

## 6. State-machine integration

| Current state | Event/action | Resulting state |
|---|---|---|
| `READY_FOR_PROVIDER` | System creates an eligible mock attempt | `PROCESSING` |
| `PROCESSING` | Mock provider confirms success | `SUCCEEDED` |
| `PROCESSING` | Mock provider confirms failure | `FAILED` |
| `PROCESSING` | Timeout or ambiguous response | `UNKNOWN` |
| `UNKNOWN` | Operations initiates inquiry; mock provider confirms success | `SUCCEEDED` |
| `UNKNOWN` | Operations initiates inquiry; mock provider confirms failure | `FAILED` |
| `UNKNOWN` | Inquiry is inconclusive | `UNKNOWN` |

Additional invariants:

- `SUCCEEDED`, `FAILED`, and `REJECTED` remain terminal.
- `UNKNOWN` does not allow a new attempt.
- An inquiry is not a dispatch.
- A transfer in `COMPLIANCE_HOLD` cannot be dispatched.
- No automatic retry is permitted.
- All state transitions must be validated server-side.

## 7. Proposed data model

| Entity/field | Purpose |
|---|---|
| `providerAttemptId` | Unique ID for one logical mock-provider attempt |
| `transferId` | Transfer associated with the attempt |
| `idempotencyKey` | Prevent duplicate logical attempts |
| `requestFingerprint` | Detect materially different requests using the same key |
| `attemptStatus` | Internal attempt lifecycle status |
| `providerReference` | Safe synthetic reference returned by the mock provider |
| `outcome` | Confirmed success, confirmed failure, or uncertain/inconclusive |
| `createdAt` / `updatedAt` | Server-generated timestamps |
| `lastInquiryAt` | Timestamp of the latest status inquiry, if any |
| `actorId` | System or authorised Operations actor for the relevant event |
| `transferVersion` | Optional version used for concurrency protection |

Implementation notes:

- Keep provider-attempt history separate from transfer status history.
- A transfer can have multiple audit events, but under this MVP policy it cannot have a second dispatch attempt.
- Generate timestamps and system identity on the server.
- Do not store secrets or unnecessary sensitive data in attempt records or logs.

## 8. Proposed API endpoints

The following endpoints are proposed for the prototype. Each must enforce authentication, backend permissions, state validation, and audit requirements.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/v1/operations/transfers/{id}/provider-attempt` | View permitted attempt details |
| `POST` | `/api/v1/operations/transfers/{id}/provider-status-inquiry` | Initiate a controlled inquiry for an `UNKNOWN` transfer |
| `GET` | `/api/v1/transfers/{id}/status` | Return customer-facing status, limited to the customer's own transfer |

Provider dispatch must be invoked by the backend workflow, not exposed as an unrestricted customer-facing or generic state-update endpoint.

The exact response schemas and error codes may be defined during implementation without weakening these business rules.

## 9. Validation and failure behaviour

- **Unauthenticated request:** reject without changing state.
- **Unauthorised request:** reject without changing state or disclosing restricted information.
- **Transfer not in `READY_FOR_PROVIDER`:** reject a new dispatch.
- **Active Compliance hold:** block dispatch.
- **Duplicate logical request:** return the existing attempt/result.
- **Same idempotency key with different request data:** return a conflict.
- **Timeout or ambiguous response:** transition to `UNKNOWN`; do not retry.
- **Inquiry for a transfer not in `UNKNOWN`:** reject or handle only through a separately defined, valid workflow.
- **Inconclusive inquiry:** remain `UNKNOWN`.
- **Manual attempt to set an outcome:** reject.
- **Audit persistence failure:** roll back the associated state change.
- **Attempt to create a new attempt while `UNKNOWN`:** reject.

## 10. Acceptance criteria

BR-06 is satisfied when all of the following are demonstrable:

1. A transfer in `READY_FOR_PROVIDER` can enter `PROCESSING` through the system-controlled workflow.
2. Mock outcomes are deterministic and visibly labelled as simulated.
3. Duplicate logical requests do not create duplicate provider attempts.
4. Reusing an idempotency key with different request data returns a conflict.
5. A confirmed success transitions to `SUCCEEDED`.
6. A confirmed failure transitions to `FAILED`.
7. A timeout or ambiguous response transitions to `UNKNOWN`.
8. Operations can initiate an inquiry for an `UNKNOWN` transfer without creating a new attempt.
9. Confirmed inquiry results resolve the transfer; inconclusive results leave it `UNKNOWN`.
10. No automatic retry or new attempt is allowed under this MVP specification.
11. Customers and Operations cannot arbitrarily set provider outcomes.
12. Attempt and inquiry history is durable and append-only.
13. Concurrent dispatch requests cannot create duplicate logical attempts.
14. Audit failures prevent associated state changes from committing.
15. No real payment provider or real-money movement is involved.

## 11. Edge-case test matrix

| Scenario | Expected behaviour |
|---|---|
| Dispatch requested for a transfer not in `READY_FOR_PROVIDER` | Reject; no new attempt |
| Transfer has an active Compliance hold | Block dispatch |
| Two dispatch requests arrive concurrently | One logical attempt at most |
| Same idempotency key and same logical request are repeated | Return original attempt/result |
| Same key is reused with different request data | Conflict |
| Mock provider returns confirmed success | Transition to `SUCCEEDED` |
| Mock provider returns confirmed failure | Transition to `FAILED` |
| Provider response times out | Transition to `UNKNOWN`; no retry |
| Operations inquires about an `UNKNOWN` transfer | Query existing attempt |
| Inquiry confirms success | Transition to `SUCCEEDED` |
| Inquiry confirms failure | Transition to `FAILED` |
| Inquiry remains inconclusive | Remain `UNKNOWN` |
| Customer tries to initiate inquiry or set outcome without permission | Reject |
| Operations tries to manually declare success/failure | Reject |
| Application loses the provider response | Recover by querying the same attempt |
| Audit persistence fails | Roll back associated state transition |
| A new attempt is requested for `UNKNOWN` | Reject |
| A second attempt is requested after confirmed failure | Reject under MVP policy; no retry feature |

## 12. Implementation guidance for the five-day prototype

- Implement a `MockProvider` interface and a local deterministic adapter.
- Store attempt records and idempotency keys in PostgreSQL.
- Use a unique constraint to enforce attempt idempotency.
- Keep provider calls outside long-running database transactions.
- Use controlled status inquiries to recover uncertain outcomes.
- Use explicit domain methods for dispatch, outcome recording, and status inquiry.
- Protect Operations endpoints with backend permissions.
- Add integration tests for timeouts, duplicate requests, concurrency, state transitions, inquiry results, and audit atomicity.
- Label mock results and sample data as simulated in the UI and documentation.

These implementation notes describe the approved prototype behaviour and do not imply production readiness.

## 13. Explicit out-of-scope items

BR-06 excludes:

- Real payment-provider integrations.
- Real payment execution, settlement, disbursement, or custody of funds.
- Automatic retries.
- New attempts after a confirmed failure.
- Random mock outcomes.
- Manual Operations overrides of provider outcomes.
- Automatic inquiry after timeout; an explicit Operations inquiry is required.
- Production-grade payment orchestration or claims of financial-service readiness.

## 14. Approval record

**BR-06 status: Approved by the user.**

This document captures the approved Mock Provider Processing, Idempotency, and Outcome Reconciliation specification for the BorderlessOS educational prototype.
