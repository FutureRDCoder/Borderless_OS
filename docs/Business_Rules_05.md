# BorderlessOS — Business Rules 05
## Operations Review and Transfer Decisioning

**Document status:** Approved  
**Project:** BorderlessOS — Cross-Border Remittance & Compliance Operations Platform  
**Scope:** Five-day educational prototype  
**Version:** 1.0

---

## 1. Purpose

This document defines BR-05 for BorderlessOS: the Operations review queue, routine review checks, missing-information requests, transfer rejection, readiness decisions, and related permissions, state transitions, and audit requirements.

BorderlessOS is an educational prototype only. It is not a licensed financial product or certified compliance system. It must not move, custody, disburse, or settle real funds. All transfer and provider activities described here are simulated.

## 2. Business objective

Ensure every submitted transfer passes through a controlled Operations workflow before it can become ready for the simulated provider workflow. Decisions must be permission-controlled, based on the approved transfer snapshot, validated by the backend, and recorded in an auditable history.

## 3. Roles and permissions

| Role | BR-05 permissions |
|---|---|
| **CUSTOMER** | May view their own permitted transfer status and respond to a missing-information request. Cannot perform Operations review decisions. |
| **OPERATIONS** | May view the permitted Operations queue and review information, request missing information, reject eligible transfers with a reason, and mark eligible transfers ready after routine checks. |
| **COMPLIANCE** | Controls Compliance hold decisions under BR-04. Compliance clearance returns the transfer to `UNDER_REVIEW`; it does not itself mark the transfer ready. |
| **ADMIN** | Has only explicitly granted application-administration permissions. Admin status alone does not grant Operations decision authority, Compliance authority, or arbitrary state override. |
| **SYSTEM** | Performs defined workflow transitions such as accepting a submitted transfer into `UNDER_REVIEW`. System actions must be distinguishable from human actions in the audit history. |

**Security requirement:** Enforce permissions in the backend. Frontend visibility alone is not authorisation.

## 4. Business rules

### BR-05.1 — Operations review queue

- Operations users may access transfers assigned to or eligible for the Operations queue.
- The queue must display only information needed for operational workflow review.
- Compliance-only details and internal compliance reasoning must remain restricted under BR-04.
- Customers may access only their own transfers and permitted customer-facing information.

### BR-05.2 — Backend-enforced review permissions

- Only authenticated users with the Operations permission may perform Operations review decisions.
- Customers cannot approve, reject, or otherwise make Operations decisions on their own transfers.
- Admin status alone does not grant Operations or Compliance decision authority.
- The backend must reject unauthorised attempts without changing the transfer state.

### BR-05.3 — Routine review checks

- Operations reviews the submitted transfer snapshot, including beneficiary details, send amount and source currency, destination currency and estimated destination amount, quote details, and required workflow information.
- Review checks are illustrative operational checks only.
- The prototype does not verify real bank accounts, perform real identity checks, or provide real compliance screening.
- The backend must validate required data and state invariants before allowing the transfer to become ready.

### BR-05.4 — Request missing information

- Operations may request missing information only while the transfer is in `UNDER_REVIEW`.
- The request must contain a customer-facing message with at least one non-whitespace character.
- A valid request transitions the transfer to `NEEDS_INFO`.
- The request message and transition must be recorded in the audit/history.
- The customer may respond to the request through the defined customer workflow. A valid response transitions the transfer to `SUBMITTED`; the system then returns it to `UNDER_REVIEW` through the controlled workflow.
- A customer response does not automatically approve the transfer or make it ready.
- Operations cannot use a missing-information request to bypass a Compliance hold.

### BR-05.5 — Reject a transfer

- Operations may reject a transfer only while it is in `UNDER_REVIEW`.
- Rejection requires a mandatory reason containing at least one non-whitespace character.
- A valid rejection transitions the transfer to `REJECTED`.
- The reason, actor, timestamp, previous state, and resulting state must be recorded in the audit history.
- `REJECTED` is terminal under the approved state machine. The customer must create a new transfer rather than reopen or edit the rejected transfer.
- A transfer in `COMPLIANCE_HOLD` cannot be rejected through an ordinary Operations action unless a separate, explicit business rule is approved; this BR-05 specification does not grant such a path.

### BR-05.6 — Mark a transfer ready for the simulated provider workflow

- Operations may transition a transfer from `UNDER_REVIEW` to `READY_FOR_PROVIDER` only after all defined routine checks are complete and required information is present.
- No Compliance hold may be active.
- The backend must enforce the transition even if a caller bypasses the frontend.
- Marking a transfer ready does not itself initiate a mock-provider attempt, move funds, or imply that a real payment has been authorised.
- A later system-controlled step is responsible for initiating the mock-provider workflow under the approved state machine and provider-workflow rules.

### BR-05.7 — Compliance holds cannot be bypassed

- Operations cannot maintain, clear, or override `COMPLIANCE_HOLD`; these actions remain governed by BR-04.
- A transfer in `COMPLIANCE_HOLD` cannot be marked `READY_FOR_PROVIDER` or dispatched to the mock provider.
- After Compliance clears the hold, the transfer returns to `UNDER_REVIEW` and must undergo the required Operations checks again.
- No alternate endpoint, retry path, UI action, or administrative action may bypass this restriction.

### BR-05.8 — Audit every Operations decision

- Record each Operations action and associated state transition in a durable audit/history record.
- At minimum, record the transfer ID, event/action type, actor ID, server-generated timestamp, previous state, resulting state, and reason or customer-facing request message where applicable.
- Audit history is append-only from the application's business perspective; new actions must not overwrite previous actions.
- A state change and its required audit record must commit atomically. If audit persistence fails, the state change must not commit.
- Logs must not contain secrets or unnecessary sensitive information.

### BR-05.9 — Preserve submission-time snapshots

- Operations must review the beneficiary and quote snapshots captured at submission under BR-03.
- Later edits to the beneficiary or the creation of a new quote must not silently change the historical details of an already submitted transfer.
- Any new customer response or workflow event must be recorded separately without rewriting the original submission snapshot.

### BR-05.10 — Enforce valid transitions and concurrency

- The backend must validate the current state, actor permission, required data, and allowed transition for every Operations action.
- There must be no generic endpoint that allows a caller to assign an arbitrary transfer state.
- Stale or conflicting decisions must be rejected or returned as a conflict rather than silently overwriting a newer decision.
- Use an appropriate concurrency strategy, such as optimistic version checks or row-level locking.
- The transfer state and required audit record must be written transactionally.

## 5. State-machine integration

| Current state | Action/event | Required actor | Resulting state |
|---|---|---|---|
| `SUBMITTED` | System accepts transfer into the review workflow | System | `UNDER_REVIEW` |
| `UNDER_REVIEW` | Request missing information with valid message | Operations | `NEEDS_INFO` |
| `NEEDS_INFO` | Customer supplies a valid response | Customer | `SUBMITTED` |
| `SUBMITTED` | System re-enters the review workflow | System | `UNDER_REVIEW` |
| `UNDER_REVIEW` | Reject with non-blank reason | Operations | `REJECTED` |
| `UNDER_REVIEW` | Routine checks complete; no active hold | Operations | `READY_FOR_PROVIDER` |
| `UNDER_REVIEW` | Synthetic illustrative compliance trigger | System workflow | `COMPLIANCE_HOLD` |
| `COMPLIANCE_HOLD` | Compliance clears hold with reason | Compliance | `UNDER_REVIEW` |

Additional constraints:

- `COMPLIANCE_HOLD` blocks readiness and mock dispatch.
- Clearing a hold does not skip Operations review.
- `REJECTED`, `SUCCEEDED`, and `FAILED` remain terminal according to the approved state machine.
- `UNKNOWN` requires controlled inquiry/reconciliation; no blind retry is permitted.
- All transitions must be enforced in backend domain logic.

## 6. Proposed API surface

The following endpoints are proposed for the prototype. Each requires authentication, backend authorisation, validation, state-transition checks, and appropriate audit records.

| Method | Endpoint | Purpose and constraints |
|---|---|---|
| `GET` | `/api/v1/operations/transfers` | List transfers visible to Operations, with restricted compliance fields omitted. |
| `GET` | `/api/v1/operations/transfers/{id}` | Retrieve permitted Operations review details. |
| `POST` | `/api/v1/operations/transfers/{id}/request-info` | Request missing information with a mandatory non-blank customer-facing message. |
| `POST` | `/api/v1/operations/transfers/{id}/reject` | Reject an `UNDER_REVIEW` transfer with a mandatory non-blank reason. |
| `POST` | `/api/v1/operations/transfers/{id}/mark-ready` | Move an eligible `UNDER_REVIEW` transfer to `READY_FOR_PROVIDER` after validated routine checks. |

The exact payload schemas and error-code conventions can be specified during implementation. They must not weaken the business rules above.

## 7. Validation and failure behaviour

- **Unauthenticated request:** reject without changing state.
- **Unauthorised role:** reject without changing state or disclosing restricted compliance information.
- **Missing/blank request message:** reject the missing-information action.
- **Missing/blank rejection reason:** reject the rejection action.
- **Invalid current state:** reject or return a conflict; no state change.
- **Required review information absent:** do not mark the transfer ready.
- **Active Compliance hold:** block readiness and provider dispatch.
- **Stale/concurrent action:** reject or return a conflict; preserve the committed state and history.
- **Audit persistence failure:** roll back the associated state change.
- **Attempt to modify a submitted snapshot through a review action:** reject; preserve the snapshot.
- **Attempt to use a generic arbitrary-state update:** reject; no unrestricted state override is allowed.

## 8. Acceptance criteria

BR-05 is satisfied when all of the following are demonstrable:

1. Operations can retrieve an eligible review queue and see only permitted information.
2. An Operations user can request missing information with a non-blank message, causing `UNDER_REVIEW` → `NEEDS_INFO`.
3. A customer can submit a valid response, causing `NEEDS_INFO` → `SUBMITTED`, followed by the controlled system transition to `UNDER_REVIEW`.
4. A customer response does not automatically approve or mark the transfer ready.
5. Operations can reject an `UNDER_REVIEW` transfer only with a valid reason; it becomes terminal `REJECTED`.
6. Operations can mark an eligible transfer ready only after required routine checks are complete and no hold is active.
7. A transfer in `COMPLIANCE_HOLD` cannot be marked ready or dispatched.
8. Compliance clearance returns the transfer to `UNDER_REVIEW`, where Operations checks must be performed again.
9. Admin status alone does not grant Operations or Compliance decision authority.
10. Every decision has a durable, append-only audit record.
11. Failed audit persistence prevents the corresponding state transition from committing.
12. Stale or concurrent actions cannot silently overwrite state or history.
13. Submission-time beneficiary and quote snapshots remain unchanged.
14. All workflow and provider behaviour remains simulated; no real-money movement or real compliance screening occurs.

## 9. Edge-case test matrix

| Scenario | Expected behaviour |
|---|---|
| Customer attempts to reject or mark ready | Reject; no state change |
| Admin attempts an Operations action without the permission | Reject; no state change |
| Operations requests information with a blank message | Reject; no state change |
| Operations requests information from a state other than `UNDER_REVIEW` | Reject or conflict |
| Customer responds while transfer is not in `NEEDS_INFO` | Reject or conflict |
| Customer responds to a valid request | Move to `SUBMITTED`; system re-enters `UNDER_REVIEW` |
| Operations rejects without a valid reason | Reject the action |
| Operations tries to reject a transfer in `COMPLIANCE_HOLD` | Reject under this specification |
| Operations marks ready while required information is missing | Reject |
| Operations marks ready while a Compliance hold is active | Reject; no provider attempt |
| Compliance clears a hold | Return to `UNDER_REVIEW`; do not mark ready automatically |
| A stale Operations action races with another decision | Reject/conflict; preserve latest state and history |
| Audit persistence fails | Roll back state transition |
| Beneficiary is edited after submission | Preserve the submission-time snapshot |
| A caller attempts an arbitrary state update | No unrestricted state-update operation; reject |

## 10. Implementation guidance for the five-day prototype

- Implement the Operations workflow in the existing modular-monolith Spring Boot backend.
- Keep transition validation in the transfer domain/service layer.
- Enforce role checks at the API/security layer and validate business permissions again in the service layer.
- Use explicit service operations for `requestMissingInformation`, `rejectTransfer`, and `markReadyForProvider`.
- Coordinate state transitions and audit entries transactionally.
- Use optimistic locking or row-level locking to handle concurrent decisions.
- Return role-appropriate DTOs so compliance-only details cannot leak into Operations responses.
- Add integration tests for permissions, required messages/reasons, valid and invalid transitions, hold blocking, snapshot immutability, audit atomicity, and concurrency.
- Label synthetic data and provider behaviour as simulated in the UI and demo documentation.

These implementation notes describe the approved prototype behaviour and do not imply production readiness.

## 11. Explicit out-of-scope items

BR-05 does not include:

- Real bank-account verification.
- Real identity verification, KYC, sanctions screening, or fraud detection.
- Real payment execution or settlement.
- Unrestricted manual state overrides.
- Customer cancellation or editing of a submitted transfer.
- Automatic approval solely because a customer supplied additional information.
- Operations authority to clear a Compliance hold.
- A production case-management, assignment, SLA, escalation, or maker-checker system unless separately approved.

## 12. Approval record

**BR-05 status: Approved by the user.**

This document captures the approved Operations Review and Transfer Decisioning specification for the BorderlessOS educational prototype.
