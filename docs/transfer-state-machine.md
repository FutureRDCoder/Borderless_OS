# BorderlessOS — Transfer State Machine

**Project:** BorderlessOS — Cross-Border Remittance & Compliance Operations Platform  
**Document:** `transfer-state-machine.md`  
**Version:** 1.0  
**Status:** Approved for the five-day educational prototype  
**Scope:** Day 1 — Step 3: Transfer state machine and transition rules

---

## 1. Purpose

This document defines the proposed transfer lifecycle for BorderlessOS, including permitted statuses, authorised triggers, exceptional outcomes, and controls that prevent invalid workflow transitions.

The state machine is designed for an educational prototype. It does not initiate, custody, disburse, settle, or otherwise move real funds. Provider outcomes are simulated.

## 2. State-machine overview

The normal customer and operations journey is:

`DRAFT` → `SUBMITTED` → `UNDER_REVIEW` → `READY_FOR_PROVIDER` → `PROCESSING` → a provider outcome.

The review workflow can branch into `NEEDS_INFO`, `COMPLIANCE_HOLD`, or `REJECTED`. An uncertain provider outcome enters `UNKNOWN` and must be reconciled before it can be resolved.

### States

| State | Meaning |
|---|---|
| `DRAFT` | Customer is preparing a transfer request. |
| `SUBMITTED` | Customer has submitted the request and it has passed initial submission validation. |
| `UNDER_REVIEW` | Request is in the operations review workflow. |
| `NEEDS_INFO` | Operations has requested missing information from the customer. |
| `COMPLIANCE_HOLD` | The request is blocked pending an authorised compliance decision. |
| `READY_FOR_PROVIDER` | Required workflow checks are complete and the request is eligible for mock-provider processing. |
| `PROCESSING` | A mock-provider attempt has been initiated and its outcome is being handled. |
| `UNKNOWN` | The provider outcome cannot currently be established. This does not mean the transfer failed. |
| `SUCCEEDED` | The mock provider has confirmed success. |
| `FAILED` | The mock provider has confirmed failure. |
| `REJECTED` | Operations has rejected the request with a recorded reason. |

## 3. Permitted transitions

| Current state | Trigger / authorised actor | Next state | Required guard or record |
|---|---|---|---|
| `DRAFT` | Customer submits a valid request | `SUBMITTED` | Validate required fields, ownership, and applicable business rules. |
| `SUBMITTED` | System accepts the request into the review workflow | `UNDER_REVIEW` | Persist the request and record the status event. |
| `UNDER_REVIEW` | Operations requests missing information | `NEEDS_INFO` | Record the requested information and a reason. |
| `NEEDS_INFO` | Customer supplies the requested information | `SUBMITTED` | Validate the response; return to review rather than auto-approving. |
| `UNDER_REVIEW` | Operations rejects the request | `REJECTED` | A nonblank rejection reason is mandatory. |
| `UNDER_REVIEW` | An illustrative compliance rule triggers a hold | `COMPLIANCE_HOLD` | Record the trigger and hold event; block dispatch. |
| `COMPLIANCE_HOLD` | Compliance maintains the hold | `COMPLIANCE_HOLD` | Record the decision and a mandatory reason. |
| `COMPLIANCE_HOLD` | Compliance clears an eligible hold | `UNDER_REVIEW` | Require the `COMPLIANCE` role and a nonblank reason; return to review for remaining checks. |
| `UNDER_REVIEW` | Operations confirms routine checks are complete and no hold remains | `READY_FOR_PROVIDER` | Validate workflow prerequisites and absence of an active hold. |
| `READY_FOR_PROVIDER` | System initiates an authorised mock-provider attempt | `PROCESSING` | Enforce dispatch eligibility and idempotency protections. |
| `PROCESSING` | Mock provider confirms success | `SUCCEEDED` | Accept a valid, deduplicated outcome event. |
| `PROCESSING` | Mock provider confirms failure | `FAILED` | Accept a valid, deduplicated outcome event. |
| `PROCESSING` | Outcome cannot be established | `UNKNOWN` | Record uncertainty; do not assume failure. |
| `UNKNOWN` | Controlled inquiry or reconciliation confirms success | `SUCCEEDED` | Record the inquiry/reconciliation action and supporting outcome. |
| `UNKNOWN` | Controlled inquiry or reconciliation confirms failure | `FAILED` | Record the inquiry/reconciliation action and supporting outcome. |
| `UNKNOWN` | Inquiry remains inconclusive | `UNKNOWN` | Record the inquiry result; keep the request unresolved. |

## 4. Terminal and exceptional states

For the initial five-day MVP:

- `REJECTED`, `SUCCEEDED`, and `FAILED` are terminal states.
- A rejected request is not reopened. The customer must create a new request if they wish to try again; the original record and history remain intact.
- `COMPLIANCE_HOLD` blocks provider dispatch until an authorised clearance is recorded.
- `UNKNOWN` is an unresolved state, not a synonym for failure.
- An inconclusive inquiry leaves the request in `UNKNOWN`.
- Reconciliation must not initiate another provider dispatch.
- Invalid transitions must be rejected by the backend without changing the transfer state.

## 5. Non-negotiable workflow controls

### 5.1 Compliance holds block dispatch

A request in `COMPLIANCE_HOLD` cannot move to `READY_FOR_PROVIDER` or `PROCESSING`. Only an authorised Compliance user can clear an eligible hold, and a reason is mandatory. The `ADMIN` role does not override this restriction.

### 5.2 Role separation

Operations cannot clear compliance holds. Admin cannot bypass workflow restrictions. The backend must validate both the actor's permissions and the transfer's current state for every consequential action.

### 5.3 Auditable transitions

Record consequential transitions with, at minimum:

- Transfer identifier.
- Previous and new state.
- Actor or system identity.
- Timestamp.
- Action/event type.
- Reason when required.
- Relevant correlation or idempotency reference where applicable.

Workflow history should be append-only at the application level; ordinary application actions must not silently rewrite prior events.

### 5.4 Unknown provider outcomes

A timeout can mean that the request was received but the response was lost. If the outcome cannot be established, set the transfer to `UNKNOWN`. Do not treat it as a confirmed failure, and do not automatically dispatch again. Use a controlled status inquiry or manual reconciliation before resolving the outcome.

### 5.5 Duplicate-effect prevention

Repeated submissions and duplicate provider events must not create duplicate transfer requests, provider attempts, or conflicting status transitions. Use idempotency keys, deduplication controls, and transactional state validation where appropriate.

### 5.6 Concurrency

Conflicting simultaneous actions must not silently overwrite one another. Use transactional validation and, where appropriate, optimistic locking or version checks.

## 6. Exception handling

- **Missing information:** Operations records what is needed and why. The customer supplies it; the transfer returns to `SUBMITTED` for review and is not automatically approved.
- **Compliance concern:** Record the illustrative trigger and place the transfer on hold. Clearance requires the Compliance role and a mandatory reason.
- **Invalid transition:** Reject the API request without changing the transfer state.
- **Provider timeout:** Enter `UNKNOWN` if the outcome cannot be established. No blind retry.
- **Duplicate callback:** Deduplicate the event and do not repeat an already-applied effect.
- **Concurrent review actions:** Reject or safely resolve stale/conflicting actions rather than allowing silent overwrites.
- **Unresolved reconciliation:** Keep the transfer in `UNKNOWN` and preserve the inquiry history until an outcome is established.

## 7. Role and authority assumptions

The following assumptions apply to this state machine:

1. The backend, not the frontend, validates every transition.
2. Only `COMPLIANCE` may clear a compliance hold.
3. `ADMIN` has restricted application-administration privileges and cannot arbitrarily approve transfers, clear compliance holds, or override transfer states.
4. `OPERATIONS` can access the operations queue except records restricted to compliance-only review.
5. Customers can access only their own permitted transfer information and history.
6. Every consequential decision records actor/system identity and timestamp, plus a reason where required.
7. Mock-provider outcomes are deterministic and are not real banking execution.
8. Reconciliation requires an explicitly authorised workflow and evidence or a mock result supporting the outcome.
9. Any retry mechanism is outside this initial state machine unless separately designed and protected against duplicate dispatch.

## 8. Implementation notes

- Implement transitions through a central domain/service method rather than allowing arbitrary status updates from controllers.
- Validate the current state, actor permissions, and transition guards on the server.
- Persist the state change and its audit event consistently, preferably in one database transaction.
- Apply idempotency controls to transfer submission, mock dispatch, and provider event handling.
- Test both allowed transitions and forbidden transitions.
- Test customer ownership isolation and role separation.
- Test duplicate callbacks, concurrent actions, and unresolved provider timeouts.
- Keep mock-provider behavior deterministic so the end-to-end demonstration is repeatable.

## 9. Acceptance criteria

The state-machine implementation is acceptable when:

- Every permitted transition in Section 3 is represented and validated.
- Unlisted transitions are rejected.
- Rejected requests remain terminal.
- Compliance holds prevent provider dispatch until authorised clearance.
- Compliance clearance requires the correct role and a reason.
- Provider success, confirmed failure, and uncertain timeout can be demonstrated.
- `UNKNOWN` is resolved only after a controlled inquiry or reconciliation establishes an outcome.
- Reconciliation does not redispatch the transfer.
- Status history records consequential transitions and required reasons.
- Duplicate events and repeated requests do not create duplicate workflow effects.
- Critical transition and authorisation tests pass.

---

**Document note:** This specification records the approved Day 1 Step 3 decisions. Business-rule details and further exception-handling requirements will be elaborated in Day 1 Step 4.
