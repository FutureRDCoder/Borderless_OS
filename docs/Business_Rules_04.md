# BorderlessOS — Business Rules 04
## Compliance Holds, Decisions, Restricted Visibility, and Auditability

**Document status:** Approved  
**Project:** BorderlessOS — Cross-Border Remittance & Compliance Operations Platform  
**Scope:** Five-day educational prototype  
**Version:** 1.0

---

## 1. Purpose

This document defines BR-04 for BorderlessOS: how illustrative compliance holds are raised, reviewed, maintained, cleared, audited, and kept separate from ordinary operations workflow.

BorderlessOS is an **educational prototype only**. It is not a licensed financial product or a certified compliance system. It must not move, custody, disburse, or settle real funds. The rules in this document describe simulated workflow behaviour, not real-world sanctions screening, identity verification, legal compliance, or regulatory determinations.

## 2. Business objective

Provide a controlled, auditable demonstration of how a transfer may be placed on hold for illustrative compliance review, how an authorised Compliance user records a reasoned decision, and how permissions prevent unauthorised users or workflow paths from bypassing the hold.

The system must favour safe failure: a technical failure must never silently clear a hold, bypass the hold, or commit a compliance decision without its required audit record.

## 3. Roles and permissions

| Role | BR-04 permissions |
|---|---|
| **CUSTOMER** | May see their own permitted transfer status and customer-facing information. Must not see internal compliance reasoning or restricted compliance-only details. Cannot maintain or clear a hold. |
| **OPERATIONS** | May see permitted workflow information needed for operational handling. Must not see compliance-only details or internal compliance reasoning. Cannot maintain or clear a hold unless separately granted the specific Compliance permission; the MVP assumes no such grant. |
| **COMPLIANCE** | May access authorised hold queues and restricted compliance details, and may maintain or clear a hold when authenticated and authorised. A single authorised Compliance user is sufficient for the MVP. |
| **ADMIN** | Has only the restricted application-administration permissions explicitly granted. Admin status alone does not grant Compliance decision authority or permission to override transfer states. |
| **SYSTEM** | May record a synthetic trigger or perform a specifically authorised workflow transition using a system identity. Automated activity must be distinguishable from a human decision. |

**Security requirement:** Enforce these permissions in the backend on every relevant request. Hiding a button or page in the frontend is not authorisation.

## 4. Business rules

### BR-04.1 — Synthetic triggers are demonstrative

- The prototype may use deterministic, synthetic rules to flag a transfer for an illustrative compliance hold.
- The interface and documentation must clearly label these triggers and outcomes as **simulated**.
- A trigger must be evaluated and applied through backend-controlled workflow logic; a client must not set `COMPLIANCE_HOLD` directly.
- The system must record the trigger or corresponding system event in the transfer's history.
- Synthetic rules must not be presented as actual sanctions screening, fraud detection, identity verification, or legal/compliance advice.

### BR-04.2 — Only authorised Compliance users may decide

- Only an authenticated user with the explicit Compliance permission may maintain or clear a hold.
- CUSTOMER, OPERATIONS, and ADMIN users cannot make these decisions merely because they are logged in or have another role.
- For the MVP, one authorised Compliance user is sufficient; dual approval is not required.
- The backend must reject unauthorised attempts and must not change the transfer state.

### BR-04.3 — Reasons are mandatory

- Both **Maintain Hold** and **Clear Hold** require a reason.
- A reason must contain at least one non-whitespace character after trimming.
- Missing, empty, or whitespace-only reasons must be rejected by the backend.
- The reason must be persisted with the decision and its audit event.
- The reason is internal compliance information and must be protected by the visibility rules in BR-04.8.

### BR-04.4 — Maintaining a hold preserves the hold state

- An authorised Compliance user may record a **Maintain Hold** decision only while the transfer is in `COMPLIANCE_HOLD`.
- The transfer remains in `COMPLIANCE_HOLD`.
- The decision appends a new history/audit record; it must not overwrite prior decisions, reasons, trigger records, or timestamps.

### BR-04.5 — Clearing a hold returns the transfer to review

- An authorised Compliance user may clear a hold only while the transfer is in `COMPLIANCE_HOLD`.
- A successful clear decision transitions the transfer to `UNDER_REVIEW`.
- Clearing a hold does **not** authorise provider dispatch and does not automatically move the transfer to `READY_FOR_PROVIDER`.
- Operations must complete the applicable ordinary workflow checks before a later, separately authorised transition to `READY_FOR_PROVIDER`.

### BR-04.6 — A hold blocks dispatch

- A transfer in `COMPLIANCE_HOLD` must not enter `READY_FOR_PROVIDER` or initiate a mock-provider attempt.
- This restriction must be enforced server-side, including when a caller invokes an endpoint directly or bypasses the frontend.
- No alternate endpoint, generic state-update operation, retry path, or administrative action may bypass the hold.
- A blocked dispatch attempt must not initiate a provider call. Record a security/workflow event where appropriate without exposing restricted compliance details to unauthorised roles.

### BR-04.7 — No arbitrary state override

- There must be no generic endpoint or privileged UI action that allows users to assign an arbitrary transfer state.
- State changes must use named, authorised business operations and validate the current state, actor permission, required data, and allowed transition.
- ADMIN does not receive a blanket exception.
- Any operational correction or reconciliation must use its own explicitly authorised workflow; it must not be implemented as a hidden state override.

### BR-04.8 — Restricted compliance visibility

- Compliance-only details and internal compliance reasoning are visible only to authorised Compliance users and other explicitly authorised principals, if any are added through a documented permission decision.
- OPERATIONS may receive only the minimum permitted information needed to continue the operational workflow. The MVP must not expose internal compliance reasoning to Operations.
- CUSTOMER-facing status and messages must not disclose internal compliance reasoning.
- ADMIN access to compliance details is not automatic. It requires explicit, documented authorisation.
- Enforce restrictions in API responses and backend access checks, not only in frontend rendering. Apply the same restrictions to list endpoints, detail endpoints, search, exports, error messages, and logs where applicable.
- Return a safe, non-sensitive status or message to roles that are not authorised to see the underlying details.

### BR-04.9 — Append-only decision and transition history

- Every hold trigger and Compliance decision must create a durable history/audit record.
- At minimum, record:
  - transfer identifier;
  - event/decision identifier;
  - event type or decision (`TRIGGERED`, `MAINTAIN_HOLD`, `CLEAR_HOLD`, or an equivalent clearly defined event type);
  - actor identifier or explicit system identity;
  - timestamp;
  - previous state and resulting state;
  - reason for a human decision, where applicable;
  - trigger type and simulated-rule identifier for automated triggers, where applicable.
- History is append-only from the application's business perspective. New decisions must not replace or erase earlier events.
- A state change and its required audit record must be committed atomically. If the audit record cannot be persisted, the associated state change must not commit.
- Audit logs must not contain secrets or unnecessary sensitive data.

### BR-04.10 — Reopening a cleared hold is controlled

- After a hold has been cleared and the transfer has returned to `UNDER_REVIEW`, only an explicitly authorised Compliance workflow may place that transfer back into `COMPLIANCE_HOLD`.
- The synthetic trigger must not automatically reopen a previously cleared hold.
- Any authorised re-hold must create a new event, retain all prior history, and record the actor/system identity, timestamp, reason or trigger details, and state transition.
- No ordinary Operations, Customer, or Admin action may reopen the hold without the explicit Compliance permission and a defined business operation.

### BR-04.11 — Fail safely and handle concurrency

- If synthetic-trigger evaluation fails, the system must not silently clear a hold, treat the transfer as cleared, or allow provider dispatch on the assumption that no hold exists.
- If audit persistence fails, the related hold decision or state change must fail atomically.
- If two decisions race, the backend must validate the current state/version and ensure only a valid, consistent transition is committed. A stale or conflicting decision must be rejected or returned as a conflict; it must not overwrite the newer state or audit history.
- Use a transaction and an appropriate concurrency strategy, such as optimistic version checks or row-level locking, for the state transition and its audit record.
- Errors returned to unauthorised roles must not reveal restricted compliance information.

### BR-04.12 — Educational simulation boundary

- All compliance triggers and decisions are illustrative workflow demonstrations.
- The prototype must not claim to provide real sanctions screening, KYC/identity verification, transaction monitoring, legal conclusions, regulatory compliance certification, or production-grade risk decisions.
- Use synthetic data only. Do not send real customer or financial data to a screening provider.
- The mock-provider workflow must remain a simulation and must not move, custody, disburse, or settle real money.

## 5. Transfer state-machine integration

The following transitions apply to BR-04:

| Current state | Action/event | Required actor | Resulting state |
|---|---|---|---|
| `UNDER_REVIEW` | Synthetic illustrative trigger | System workflow | `COMPLIANCE_HOLD` |
| `COMPLIANCE_HOLD` | Maintain hold with non-blank reason | Authorised Compliance user | `COMPLIANCE_HOLD` |
| `COMPLIANCE_HOLD` | Clear hold with non-blank reason | Authorised Compliance user | `UNDER_REVIEW` |
| `UNDER_REVIEW` after a prior clear | Explicitly authorised re-hold operation with recorded details | Authorised Compliance workflow | `COMPLIANCE_HOLD` |

Additional state-machine constraints:

- Clearing a hold returns the transfer to `UNDER_REVIEW`; it does not skip Operations review.
- A transfer in `COMPLIANCE_HOLD` cannot transition to `READY_FOR_PROVIDER` or initiate mock dispatch.
- `REJECTED`, `SUCCEEDED`, and `FAILED` remain terminal states under the approved overall state machine.
- `UNKNOWN` remains subject to controlled inquiry/reconciliation; no blind retry is permitted.
- Every transition must be validated by backend domain logic.

## 6. Proposed data model

A dedicated compliance-decision/history record should contain, at minimum:

| Field | Purpose |
|---|---|
| `decisionId` | Unique identifier for the event or decision |
| `transferId` | Transfer associated with the event |
| `decision` / `eventType` | `TRIGGERED`, `MAINTAIN_HOLD`, `CLEAR_HOLD`, or a defined re-hold event |
| `reason` | Mandatory for human maintain/clear decisions; not applicable to every system trigger |
| `actorId` | Authenticated Compliance user ID or explicit system identity |
| `createdAt` | Server-generated timestamp |
| `previousState` | State before the event |
| `resultingState` | State after the event |
| `triggerType` | Type of synthetic trigger, where applicable |
| `triggerRuleId` | Identifier for the deterministic illustrative rule, where applicable |

Implementation notes:

- Generate timestamps and actor identity on the server; do not trust client-supplied values.
- Persist decision/history records durably.
- Use database constraints and transactional writes where appropriate.
- Do not store restricted reasoning in broadly accessible transfer fields or customer-facing status text.
- Keep field-level/API response visibility aligned with role permissions.

## 7. Proposed API surface

All endpoints below are proposals for the prototype and must be protected by authentication, backend authorisation, state validation, and audit requirements.

| Method | Endpoint | Purpose and constraints |
|---|---|---|
| `GET` | `/api/v1/compliance/holds` | Return the authorised Compliance user's permitted hold queue. Do not expose restricted details to other roles. |
| `GET` | `/api/v1/compliance/transfers/{id}` | Return compliance detail for an authorised Compliance user only. |
| `POST` | `/api/v1/compliance/transfers/{id}/maintain-hold` | Record a maintain decision with a mandatory non-blank reason; allowed only in `COMPLIANCE_HOLD`. |
| `POST` | `/api/v1/compliance/transfers/{id}/clear-hold` | Record a clear decision with a mandatory non-blank reason; allowed only in `COMPLIANCE_HOLD`, transitioning to `UNDER_REVIEW`. |

The backend may use an internal system workflow operation for synthetic triggers and an explicitly authorised operation for re-holds. Such operations must not create a general-purpose state override.

Example request body for a human decision:

```json
{
  "reason": "Illustrative review reason for the educational prototype"
}
```

The reason is an example only; production-like or real personal/financial data must not be used in demo records.

## 8. Validation and failure responses

The API should return consistent, non-sensitive errors for invalid actions. Exact error codes may be standardised during implementation, but behaviour must satisfy these rules:

- **Unauthenticated request:** reject; do not change state.
- **Unauthorised role:** reject; do not change state or disclose restricted details.
- **Missing/blank reason:** reject; do not change state.
- **Invalid current state:** reject or return a conflict; do not change state.
- **Stale/concurrent decision:** reject or return a conflict; preserve the committed state and full history.
- **Audit persistence failure:** roll back the associated state change.
- **Trigger evaluation failure:** do not silently clear or bypass a hold; block any unsafe dispatch path.
- **Attempted dispatch while held:** block before provider initiation.
- **Attempt to access restricted detail:** deny or return a safe response that does not disclose the protected content.

## 9. Acceptance criteria

BR-04 is satisfied when all of the following are demonstrable:

1. A deterministic synthetic rule can place an eligible transfer on `COMPLIANCE_HOLD`, and the UI identifies the trigger as simulated.
2. A hold-trigger event is persisted with system identity, timestamp, transfer ID, and state transition details.
3. An authenticated authorised Compliance user can maintain a hold with a valid reason; the transfer remains held and a new decision record is appended.
4. An authorised Compliance user can clear a hold with a valid reason; the transfer returns to `UNDER_REVIEW`, not `READY_FOR_PROVIDER`.
5. Missing, empty, and whitespace-only reasons are rejected by the backend.
6. CUSTOMER, OPERATIONS, and ADMIN cannot maintain or clear a hold without the explicit Compliance permission.
7. A held transfer cannot be advanced to `READY_FOR_PROVIDER` or dispatched to the mock provider through any API path.
8. A generic arbitrary-state-update mechanism is absent.
9. Operations and Customers do not receive restricted compliance reasoning through APIs or UI.
10. Previous trigger and decision records remain available to authorised viewers after later decisions.
11. A failed audit write prevents the associated state change from committing.
12. Concurrent or stale decisions cannot silently overwrite state or history.
13. A synthetic trigger does not automatically reopen a previously cleared hold.
14. Terminal states remain protected according to the approved state machine.
15. All data and triggers remain demonstrative; no real screening provider or real-money movement is used.

## 10. Edge-case test matrix

| Scenario | Expected behaviour |
|---|---|
| CUSTOMER attempts maintain/clear | Reject; no state change |
| OPERATIONS attempts maintain/clear | Reject unless specifically granted Compliance permission; MVP grants none |
| ADMIN attempts maintain/clear | Reject unless specifically granted Compliance permission; admin status alone is insufficient |
| Compliance submits a blank or whitespace-only reason | Reject; no state change |
| Compliance maintains a transfer not in `COMPLIANCE_HOLD` | Reject or conflict; no state change |
| Compliance clears a transfer not in `COMPLIANCE_HOLD` | Reject or conflict; no state change |
| Compliance maintains a valid hold | Remain `COMPLIANCE_HOLD`; append decision |
| Compliance clears a valid hold | Move to `UNDER_REVIEW`; append decision |
| Dispatch is attempted while held | Block before mock-provider initiation |
| Caller tries generic arbitrary state update | No such unrestricted operation; reject attempt |
| Operations requests compliance-only details | Deny or return a safe response |
| Customer requests internal compliance reasoning | Do not disclose it |
| Two decisions arrive concurrently | Validate state/version; reject stale/conflicting action; preserve audit history |
| Trigger evaluation fails | Do not infer clearance; fail safely and prevent unsafe bypass |
| Audit write fails during decision | Roll back state change |
| A cleared transfer matches a synthetic trigger again | Do not automatically reopen; require an authorised Compliance workflow |
| A terminal transfer is targeted for a hold decision | Reject; terminal-state protection applies |

## 11. Implementation guidance for the five-day prototype

Recommended implementation approach:

- Keep the workflow in the existing modular-monolith Spring Boot backend.
- Implement explicit domain methods for `triggerHold`, `maintainHold`, `clearHold`, and any approved re-hold operation.
- Centralise transition validation in the transfer domain/service layer.
- Enforce role checks at the API/security layer and revalidate business permissions in the service layer.
- Use Spring transactions so a state update and its audit/decision record commit together.
- Use optimistic locking or row-level locking to prevent conflicting decisions.
- Use response DTOs that separate customer/operations-visible status from compliance-only details.
- Add integration tests for role enforcement, reason validation, state transitions, dispatch blocking, audit atomicity, and concurrency.
- Label all trigger rules, sample reasons, and sample transfers as illustrative/simulated in the UI and demo documentation.

These are implementation recommendations for the approved business behaviour; they do not certify production readiness.

## 12. Explicit out-of-scope items

BR-04 does **not** include:

- Real sanctions-list screening or sanctions determinations.
- Real KYC, identity verification, or document verification.
- Real fraud detection or production risk scoring.
- Legal or regulatory advice, compliance certification, or jurisdiction-specific policy implementation.
- Real payment-provider integration or movement of real funds.
- Automatic clearance based solely on a missing/failed trigger response.
- Unrestricted administrative overrides.
- Mandatory dual-control approval; one authorised Compliance user is sufficient for this MVP.

## 13. Approval record

**BR-04 status: Approved by the user.**

This document captures the approved rules for illustrative compliance holds, mandatory decision reasons, Compliance-only decision authority, restricted visibility, controlled re-holds, append-only audit history, safe failure, and the educational-prototype boundary.
