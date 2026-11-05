# BorderlessOS — Business Rules 07
## Audit Trail, Operational Visibility, and Dashboards

**Document status:** Approved  
**Project:** BorderlessOS — Cross-Border Remittance & Compliance Operations Platform  
**Scope:** Five-day educational prototype  
**Version:** 1.0

---

## 1. Purpose

This document defines BR-07 for BorderlessOS: centralised audit history, standard audit event fields, role-specific dashboards, role-appropriate data visibility, search and filtering, audit integrity, and consistent status reporting.

BorderlessOS is an educational prototype only. It is not a licensed financial product or certified compliance system. It must not move, custody, disburse, or settle real funds. All provider activity and workflow outcomes are simulated.

## 2. Business objective

Maintain one trustworthy history of important business events while presenting different, role-appropriate views to Customers, Operations, Compliance, and Admin. All dashboard data and audit access must be authorised by the backend.

## 3. Roles and visibility

| Role | BR-07 visibility |
|---|---|
| **CUSTOMER** | May view their own transfers, permitted current status, submission details, customer-facing missing-information requests, and a customer-safe timeline. Must not see other customers' data or internal Compliance reasoning. |
| **OPERATIONS** | May view the permitted review queue, routine review details, customer responses, appropriate workflow history, and permitted provider-attempt/inquiry information. Must not see Compliance-only details or internal Compliance reasoning. |
| **COMPLIANCE** | May view the authorised hold queue, restricted Compliance details, decision reasons, trigger information, and relevant transfer history. Access does not grant authority to bypass other workflow controls. |
| **ADMIN** | May view application-level operational information and explicitly authorised audit information needed for administration. Admin status alone does not grant access to restricted Compliance reasoning or authority to change business outcomes or rewrite audit history. |
| **SYSTEM** | Creates workflow and audit events under an explicit system identity. System activity must be distinguishable from human actions. |

**Security requirement:** Enforce permissions and ownership in the backend. Frontend visibility alone is not authorisation.

## 4. Business rules

### BR-07.1 — Centralised, append-only audit trail

- Record important business events, including transfer creation and submission, state transitions, beneficiary changes relevant to workflow, quote usage, missing-information requests, Operations decisions, Compliance decisions, provider attempts, status inquiries, and outcome resolutions.
- Append new events rather than overwriting earlier events.
- Audit history must remain durable and available to authorised viewers according to role permissions.

### BR-07.2 — Standard audit event fields

Each event should contain, as applicable:

- event ID;
- event type;
- relevant entity type and entity ID;
- related transfer ID, where applicable;
- actor or system identity;
- server-generated timestamp;
- previous and resulting states, where applicable;
- safe summary of the action;
- reason or other restricted details, when applicable and protected by role-visibility rules.

Reasons and restricted trigger details must not be exposed to roles that are not authorised to see them.

### BR-07.3 — Role-specific dashboards

- Provide role-appropriate dashboard views for CUSTOMER, OPERATIONS, COMPLIANCE, and ADMIN.
- Dashboard data and available actions must be based on backend-authorised data and permissions.
- The mock provider is a system workflow, not a human role.
- Dashboard summaries must not grant permissions beyond those already established by BR-01 through BR-06.

### BR-07.4 — Customer visibility

- Customers may view only their own transfers and permitted customer-facing information.
- They may view the permitted current status, submission details, and customer-facing missing-information requests.
- They may see a customer-safe transfer timeline.
- They must not see internal Compliance reasoning, restricted internal decision details, other customers' data, or unrestricted internal audit events.

### BR-07.5 — Operations visibility

- Operations can view its permitted review queue, transfer status, routine review details, customer responses, and appropriate workflow history.
- Internal Compliance reasoning and Compliance-only details remain hidden.
- Provider-attempt and inquiry information must be limited to operational fields needed for permitted work.
- Operations must not gain additional decision authority merely by viewing an event or dashboard.

### BR-07.6 — Compliance visibility

- Authorised Compliance users can view the permitted hold queue, restricted review details, decision reasons, trigger information, and relevant transfer history.
- Compliance access does not permit arbitrary state changes or bypassing the approved workflow.
- Maintain/clear decisions remain governed by BR-04.

### BR-07.7 — Admin visibility and limits

- Admin may view application-level operational information and authorised audit information needed for administration.
- Demo-user information and other administrative data may be shown only if the related capability is implemented and explicitly authorised.
- Admin status alone does not grant blanket access to Compliance reasoning.
- Admin cannot arbitrarily clear holds, change provider outcomes, override transfer states, or rewrite audit history.

### BR-07.8 — Search, filtering, and pagination

- Provide practical filters for permitted records, such as status, creation date, and transfer reference.
- Apply authorisation and ownership restrictions before returning results.
- Use bounded pagination and reasonable query limits so dashboards cannot unintentionally retrieve the entire database.
- Search results, counts, filters, exports, and error messages must not leak restricted records or fields.

### BR-07.9 — Protect audit integrity and sensitive data

- Audit events must not be editable or deletable through ordinary application endpoints.
- Never log passwords, access tokens, secrets, or unnecessary sensitive information.
- Restrict access to audit details.
- Prevent protected fields from leaking through API responses, filters, errors, logs, exports, or frontend components.
- Historical event content must not be rewritten to reflect a later decision.

### BR-07.10 — Consistent status and event history

- Current transfer status and historical events must agree.
- State transitions and their required audit records must commit atomically.
- A failed audit write must not leave a committed, unrecorded business-state change.
- Dashboard summaries must derive from authoritative backend data rather than client-side guesses.
- Use server-generated timestamps and authoritative persisted state.

## 5. Proposed dashboard contents by role

### 5.1 CUSTOMER dashboard

- Own transfers and current statuses.
- Transfer reference, submission date, and permitted amount/currency details.
- Missing-information requests and permitted response actions.
- Customer-visible transfer timeline.

### 5.2 OPERATIONS dashboard

- Eligible review queue and counts by workflow status.
- Transfers awaiting information or review.
- Permitted beneficiary and quote snapshots.
- Operations decision history.
- Permitted provider-attempt status and status-inquiry action for `UNKNOWN` transfers.

### 5.3 COMPLIANCE dashboard

- Current Compliance hold queue.
- Simulated trigger information and restricted review details.
- Maintain/clear decision actions with mandatory reasons.
- Compliance decision and hold history.

### 5.4 ADMIN dashboard

- Application-level operational health and permitted aggregate counts.
- Demo-user and explicitly authorised administrative information, if implemented.
- Permitted audit views.
- No blanket Compliance access or arbitrary state overrides.

These are proposed MVP dashboard contents, not a commitment to build a full analytics platform.

## 6. Proposed audit event model

| Field | Purpose |
|---|---|
| `eventId` | Unique audit event identifier |
| `entityType` | Entity category, such as transfer, beneficiary, quote, or provider attempt |
| `entityId` | Identifier of the affected entity |
| `transferId` | Related transfer ID when applicable |
| `eventType` | Event category, such as `TRANSFER_SUBMITTED` or `PROVIDER_INQUIRY_COMPLETED` |
| `actorId` | Authenticated actor ID or explicit system identity |
| `createdAt` | Server-generated timestamp |
| `previousState` | State before the event, where applicable |
| `resultingState` | State after the event, where applicable |
| `summary` | Safe, concise description of the event |
| `reason` | Optional restricted reason, protected by access controls |

Implementation principle: an audit event is a historical record, not an editable copy of the entity. Where reasons or trigger details are restricted, store and expose them through appropriate access-controlled fields or records.

## 7. Proposed API surface

These endpoints are candidates for implementation. All must enforce backend authorisation and return only role-permitted fields.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/v1/transfers/{id}/timeline` | Return a customer-safe timeline for the customer's own transfer |
| `GET` | `/api/v1/operations/audit-events` | Search permitted operational audit events |
| `GET` | `/api/v1/compliance/audit-events` | Search permitted Compliance audit events |
| `GET` | `/api/v1/admin/audit-events` | Retrieve audit information explicitly authorised for Admin |
| `GET` | Role-specific dashboard endpoints or views | Return permitted aggregate counts and queue summaries |

The backend must enforce ownership and role restrictions on every request. Do not rely on a shared endpoint that returns all fields and expects the frontend to hide restricted ones.

## 8. Acceptance criteria

BR-07 is satisfied when all of the following are demonstrable:

1. Important business actions produce durable audit events.
2. Events include actor/system identity, server timestamp, event type, and applicable state transitions.
3. Audit history is append-only through normal application operations.
4. A state change cannot commit without its required audit record.
5. Customers see only their own permitted data and customer-facing timeline.
6. Operations cannot retrieve internal Compliance reasoning.
7. Compliance can retrieve authorised hold and decision details.
8. Admin cannot use administrative status to bypass Compliance permissions or rewrite history.
9. Search and filters enforce ownership and role access before returning results.
10. Dashboard counts and status displays derive from backend-authoritative data.
11. Sensitive data and secrets are excluded from logs and unauthorised responses.
12. The prototype remains an educational simulation.

## 9. Edge-case test matrix

| Scenario | Expected behaviour |
|---|---|
| Customer requests another customer's transfer timeline | Deny access |
| Operations requests restricted Compliance reasoning | Deny or omit restricted fields |
| Admin attempts to clear a Compliance hold | Reject unless separately authorised; admin status alone is insufficient |
| User attempts to edit/delete an audit event through the API | Reject; no ordinary mutation endpoint |
| Audit write fails during a state transition | Roll back the state change |
| Filter query attempts to expose restricted records | Apply server-side permission and ownership filtering |
| Dashboard count conflicts with authoritative transfer state | Recompute from backend data; do not trust client calculations |
| Event contains a password, token, or unnecessary sensitive data | Do not record that information |
| A transfer's state changes multiple times | Preserve each transition in order; do not overwrite previous events |
| User requests excessive unpaginated results | Enforce bounded pagination and permitted query limits |

## 10. Implementation guidance for the five-day prototype

- Implement audit events in the existing modular-monolith Spring Boot backend.
- Use shared audit-writing services or domain event handlers where appropriate, while ensuring required audit records are committed transactionally with state changes.
- Use role-specific response DTOs and explicit access checks for customer, Operations, Compliance, and Admin views.
- Keep current state derived from persisted backend records; dashboards should not infer business outcomes in the browser.
- Implement bounded pagination and server-side filters.
- Add integration tests for ownership isolation, role-specific visibility, audit append-only behaviour, audit atomicity, and filter/pagination restrictions.
- Label synthetic records and provider outcomes as simulated in the UI and demo documentation.

These implementation notes describe the approved prototype behaviour and do not imply production readiness.

## 11. Explicit out-of-scope items

BR-07 does not include:

- A full business-intelligence or enterprise analytics platform.
- External log aggregation or a production Security Information and Event Management (SIEM) system.
- A claim that the audit trail is tamper-proof against database administrators or infrastructure-level access.
- Unlimited exports or unrestricted global search.
- Blanket Admin access to Compliance details.
- Real financial processing or real compliance screening.

## 12. Approval record

**BR-07 status: Approved by the user.**

This document captures the approved Audit Trail, Operational Visibility, and Dashboards specification for the BorderlessOS educational prototype.
