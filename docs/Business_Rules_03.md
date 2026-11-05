# BorderlessOS — Business Rules 03: Transfer Submission and Lifecycle

**Project:** BorderlessOS — Cross-Border Remittance & Compliance Operations Platform  
**Project phase:** Day 1 — Step 4: Business Rules & Exception Handling  
**Specification ID:** BR-03  
**Status:** Approved  
**Scope:** Five-day educational MVP

## 1. Purpose and scope

BR-03 defines how customers create and edit transfer drafts, submit them for review, and track transfers. It covers submission validation, quote consumption, idempotency, submission-time snapshots, role-based visibility, auditability, concurrency, and the relationship to the approved transfer state machine.

BorderlessOS is an educational prototype, not a licensed financial product or certified compliance system. It must not move, custody, disburse, or settle real funds. Any provider interaction in this prototype is simulated.

## 2. Approved product decisions

- **Draft transfers:** Persistent, editable `DRAFT` transfers.
- **Idempotency:** At least 24-hour request-key retention, with a durable association to the transfer.
- **Quote reuse:** One quote per successfully created transfer.
- **Beneficiary edits:** Preserve a submission-time snapshot; later edits do not alter the transfer.
- **Customer tracking:** Read-only after submission; no customer cancellation or modification.

## 3. Business rules

### BR-03.1 — Authenticated ownership

Only an authenticated customer may create and manage their own drafts and submit their own transfers. The backend derives ownership from the authenticated identity, never from a client-supplied `customerId`.

### BR-03.2 — Persistent draft management

Customers may save a transfer in `DRAFT` state and edit it before submission. Drafts are not yet submitted for operations review. Draft edits must pass applicable validation, and only the owner may access or change a draft.

### BR-03.3 — Submission-time validation

Before submission, the backend must validate:

- The customer owns the transfer draft and its beneficiary.
- The beneficiary is active.
- The send amount meets BR-02's configured limits and precision rules.
- Source and destination currencies are supported and compatible with the beneficiary.
- The FX quote matches the amount and currency pair and has not expired.
- The quote has not already been consumed by another successfully created transfer.

### BR-03.4 — Immutable submission snapshot

At successful submission, preserve the relevant beneficiary details, send amount, currencies, FX rate, estimated destination amount, quote identifier, and quote source. Later beneficiary edits or new quotes must not silently rewrite the submitted transfer's history.

### BR-03.5 — Controlled state transitions

A successful submission transitions through `SUBMITTED` and then `UNDER_REVIEW` using controlled backend transitions. Clients cannot set arbitrary transfer states through request payloads or direct API calls.

### BR-03.6 — Idempotent submission

Submission requires an idempotency key. Retain request-key handling for at least 24 hours and preserve a durable association between the key and the created transfer. Equivalent retries return the original result rather than creating duplicates. Reusing a key with materially different request data returns a conflict.

### BR-03.7 — Single-use quote

A quote can be consumed by at most one successfully created transfer. Quote consumption and transfer creation must be coordinated atomically to prevent concurrent requests from using the same quote.

### BR-03.8 — Read-only customer tracking

After submission, customers may view and track their transfer status and history but cannot modify or cancel the submitted transfer. Drafts remain editable until submission. Any operational changes must be performed through the authorised workflow and approved state machine.

### BR-03.9 — Role-based visibility

Customers can view only their own transfers. Operations and Compliance access must follow the previously approved role-permission matrix. Admin access does not automatically grant compliance-clearance authority.

### BR-03.10 — Auditability

Record creation and significant state changes with the actor or system component, timestamp, previous state, new state, and applicable reason. Avoid logging secrets or unnecessary sensitive data in audit records.

### BR-03.11 — Educational simulation only

Transfer submission creates a workflow record. Subsequent provider interactions use the mock provider. BorderlessOS does not move, custody, disburse, or settle real funds.

## 4. Transfer lifecycle and state-machine relationship

Persistent drafts introduce a distinction between saving a draft and submitting it. The existing approved state machine remains authoritative for the submitted workflow.

1. **`DRAFT`:** The customer can save, edit, and validate the draft. It is not yet submitted for operations review.
2. **Submission validation:** The backend validates ownership, beneficiary status, amount, currency pair, quote identity and expiry, and idempotency.
3. **`SUBMITTED`:** The transfer is accepted after successful validation.
4. **`UNDER_REVIEW`:** A controlled backend transition moves the transfer into the operations workflow.
5. **Existing approved workflow:** Continue through the approved states and transitions, including `NEEDS_INFO`, `COMPLIANCE_HOLD`, `READY_FOR_PROVIDER`, `PROCESSING`, `UNKNOWN`, and terminal outcomes.

### State-machine rules

- `DRAFT` is a persistent, customer-editable state.
- Drafts can be incomplete if the draft-save API supports partial drafts, but an incomplete draft cannot be submitted.
- Submission is permitted only after all required server-side validations pass.
- A successful submission moves to `SUBMITTED`, followed by the controlled transition to `UNDER_REVIEW`.
- Customers cannot directly set `SUBMITTED`, `UNDER_REVIEW`, or any later state.
- After submission, customers cannot cancel or edit the transfer.
- Existing `NEEDS_INFO`, `COMPLIANCE_HOLD`, `UNKNOWN`, and terminal-state behaviour remains unchanged.
- `REJECTED`, `SUCCEEDED`, and `FAILED` remain terminal under the approved state machine.
- `UNKNOWN` requires controlled inquiry or reconciliation before resolution. No blind retry is permitted.

## 5. Validation rules and edge cases

| Scenario | Expected behaviour |
|---|---|
| Customer accesses another customer's draft | Deny access. |
| Draft has missing or invalid required fields | Draft saving may allow partial data if the API supports it; block submission until valid. |
| Beneficiary is inactive at submission | Reject submission. |
| Quote has expired | Require a fresh quote before submission. |
| Quote amount or currency pair does not match the draft | Reject submission. |
| Quote has already been consumed | Reject submission unless this is an idempotent retry of the original transfer. |
| Customer double-clicks Submit | Return the same transfer result for the same idempotency key. |
| Network fails after transfer creation | Retry with the same key and return the existing transfer result. |
| Same key is reused with different request data | Return a conflict error. |
| Two requests concurrently try to consume the same quote | Only one may create a new transfer successfully. |
| Database fails partway through creation | Roll back incomplete changes; do not leave an untracked transfer or consumed quote. |
| Customer attempts to modify a submitted transfer | Reject the modification. |
| Customer attempts to cancel a submitted transfer | Reject the cancellation. |
| Beneficiary changes after submission | Preserve the original submission-time snapshot. |
| Client sends a forged state field | Reject or ignore it according to a consistent API policy; never accept the arbitrary transition. |
| User requests another customer's transfer | Deny access without disclosing private details. |
| Mock provider later returns an ambiguous outcome | Apply the approved `UNKNOWN` and reconciliation rules; do not blindly retry. |

### Draft validation clarification

A saved draft may be incomplete, but it must not enter `SUBMITTED` until all required validations pass. The API should distinguish draft-save validation from final submission validation so customers can work progressively without weakening submission controls.

## 6. Proposed data model

In addition to fields required by the approved state machine, the implementation should persist:

| Field | Purpose |
|---|---|
| `transferId` | Unique transfer identifier. |
| `customerId` | Transfer owner, derived from authenticated identity. |
| `status` | Current state in the approved state machine. |
| `beneficiaryId` | Reference to the beneficiary. |
| `beneficiarySnapshot` | Relevant recipient/destination details captured at submission. |
| `sendAmount` | Requested source amount. |
| `sourceCurrency` | Currency the customer sends. |
| `destinationCurrency` | Currency used for the estimate. |
| `quoteId` | Quote used at submission. |
| `exchangeRate` | Rate used for the indicative calculation. |
| `estimatedDestinationAmount` | Estimated amount after rounding. |
| `quoteSource` | `EXTERNAL` or `MOCK`. |
| `createdAt` | Creation timestamp. |
| `updatedAt` | Last-update timestamp. |
| `submittedAt` | Submission timestamp; null while the transfer remains a draft. |

Idempotency records should store the key, customer scope, request fingerprint, associated transfer ID, original response reference, and retention metadata.

## 7. Proposed API endpoints

These endpoint names are proposals for implementation and are not deployed APIs.

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/v1/transfers` | Create a draft. |
| `GET` | `/api/v1/transfers` | List the authenticated customer's transfers. |
| `GET` | `/api/v1/transfers/{id}` | Retrieve an owned transfer. |
| `PATCH` | `/api/v1/transfers/{id}` | Edit an owned `DRAFT` only. |
| `POST` | `/api/v1/transfers/{id}/submit` | Validate and submit a draft. |
| `GET` | `/api/v1/transfers/{id}/status` | Retrieve the current status. |

No customer cancellation endpoint is included.

## 8. Transaction, concurrency, and audit requirements

For reliable submission, coordinate the following operations within a transaction where practical:

1. Validate the draft and current beneficiary status.
2. Validate quote identity, binding, expiry, and single-use status.
3. Check the idempotency key and request fingerprint.
4. Create the immutable submission snapshot.
5. Mark the quote as consumed by the transfer.
6. Persist the submission and associated audit event.
7. Return the original transfer identifier and response.

Use database constraints and appropriate locking or optimistic concurrency controls to prevent two concurrent submissions from consuming the same quote or creating duplicates. A retry must discover the original outcome without creating another transfer.

The minimum 24-hour idempotency window applies to request-key processing. The durable transfer association remains available with the transfer record, so expiration of transient key-processing metadata must not erase the fact that the transfer was created.

## 9. Acceptance criteria

- [ ] Customers can create persistent drafts.
- [ ] Customers can edit only their own drafts.
- [ ] Invalid drafts cannot be submitted.
- [ ] Submission validates the active beneficiary, configured amount limits, currencies, and quote.
- [ ] The quote is consumed by at most one successfully created transfer.
- [ ] The transfer stores a submission-time beneficiary and quote snapshot.
- [ ] Repeated requests using the same idempotency key do not create duplicate transfers.
- [ ] Reusing an idempotency key with different request data returns a conflict.
- [ ] Idempotency request-key handling lasts at least 24 hours, with a durable transfer association.
- [ ] State transitions are controlled by the backend.
- [ ] Customers can track but cannot modify or cancel submitted transfers.
- [ ] Role-based visibility and audit requirements are enforced.
- [ ] The provider workflow remains simulated and cannot move real funds.

## 10. Safety boundaries and out of scope

- Customer cancellation or modification after submission.
- Customer-controlled transfer-state changes.
- Reusing a quote for multiple newly created transfers.
- Real financial execution, custody, disbursement, or settlement.
- Blind retry of an ambiguous mock-provider outcome.
- Treating an indicative quote as a guaranteed rate or payout.

## 11. Approval record

BR-03 was reviewed and approved by the user before proceeding to BR-04.

**Next step:** BR-04 — Compliance holds and review decisions. Work on BR-04 should begin only after this BR-03 approval has been recorded.
