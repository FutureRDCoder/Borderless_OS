# Business Rules 08 — Security, Configuration, Error Handling, and MVP Acceptance

**Project:** BorderlessOS — Cross-Border Remittance & Compliance Operations Platform  
**Document type:** Approved business-rule specification  
**Status:** Approved  
**Scope:** Five-day educational prototype  
**Authentication decision:** Spring Security with seeded demo users and securely hashed passwords

---

## 1. Purpose and boundaries

This specification defines the baseline security, configuration, error-handling, local-environment, testing, and acceptance requirements for BorderlessOS.

BorderlessOS is an educational prototype. It is **not** a licensed financial product or a certified compliance system. The application must not move, custody, disburse, or settle real funds. All users, beneficiaries, transfer data, compliance triggers, exchange-rate fallbacks, and provider outcomes used in the prototype must be synthetic or simulated.

These rules complement Business Rules BR-01 through BR-07. They do not grant any role permission to bypass the previously approved beneficiary, quote, transfer, operations, compliance, provider, or audit rules.

## 2. Authentication and session security

### BR-08.01 — Protect authenticated APIs
- All protected API endpoints require an authenticated user.
- Public access must be limited to explicitly documented endpoints needed for application operation, such as health checks, where applicable.
- Authentication failures must return a consistent, safe API error response.

### BR-08.02 — Use Spring Security and seeded demo users
- Implement authentication and authorization using **Spring Security**.
- Seed clearly identified development/demo users for the roles required by the approved workflow.
- Store demo-user passwords only as securely hashed values; never store plaintext passwords in the application database.
- Demo credentials and seed accounts are for local educational use only. They must not be presented as production-ready account provisioning.
- Do not commit real credentials, production secrets, or reusable private keys to source control.

### BR-08.03 — Do not trust client-supplied identity
- The authenticated principal on the server is the source of truth for the acting user and role.
- Never trust a client-supplied `actorId`, role, ownership field, or similar value to establish identity or permissions.
- Backend authorization is mandatory even when the frontend hides unavailable actions.

> **Implementation note:** The selected decision is Spring Security with seeded demo users and securely hashed passwords. The specific login transport or credential exchange details should be documented in the implementation design before coding; this approval does not independently select JWT or server-side session cookies.

## 3. Server-side authorization and ownership

### BR-08.04 — Enforce role permissions on every request
- Enforce the approved role-permission matrix in the backend for every relevant endpoint and resource.
- Apply authorization consistently across beneficiary management, quote creation and use, transfers, operations review, compliance decisions, mock-provider inquiries, timelines, dashboards, and audit views.
- Admin does not receive automatic compliance-clearance authority, arbitrary transfer-state override permission, or permission to bypass business rules.
- The mock provider is a system responsibility, not a human role.

### BR-08.05 — Enforce customer data isolation
- Customers may access and manage only their own beneficiaries and transfers, subject to the approved rules.
- Operations, Compliance, and Admin users may access only the data and restricted details allowed by their approved role.
- Resource ownership and role checks must be enforced server-side for both collection and individual-resource endpoints, including direct API requests.

## 4. Configuration and secret handling

### BR-08.06 — Keep environment-specific configuration outside source code
- Use environment-specific configuration for local development and test execution.
- Supply sensitive values through environment variables or an appropriate local secrets mechanism.
- Do not commit real secrets, tokens, passwords, private keys, or production connection strings.
- Clearly mark demo credentials and synthetic seed data as development-only.

### BR-08.07 — Fail safely when required configuration is missing
- Validate required configuration at startup or at the point it is needed.
- Missing or invalid required configuration must produce a clear operational error and must not silently enable unsafe behavior.
- In particular, the application must not invent an exchange rate when the external FX source and the approved deterministic mock fallback are both unavailable.
- Configuration failures must not silently clear a compliance hold, fabricate a provider outcome, or enable real-money functionality.

## 5. Consistent API errors

### BR-08.08 — Use a stable, safe error contract
API errors should include:
- a server-generated timestamp;
- an HTTP status;
- a stable application error code;
- a concise, safe message suitable for the caller;
- a request/correlation ID when available.

Example:

```json
{
  "timestamp": "server-generated timestamp",
  "status": 409,
  "code": "INVALID_TRANSFER_STATE",
  "message": "This action is not permitted in the current transfer state.",
  "requestId": "correlation-id"
}
```

The timestamp and request ID above are illustrative placeholders, not literal values.

### BR-08.09 — Do not expose internal details
- Never return stack traces, SQL details, credentials, tokens, secret configuration, or internal implementation details in API error responses.
- Do not expose restricted Compliance trigger details or internal Compliance reasoning to customers or unauthorised Operations users.
- Error messages must not reveal whether another customer's private resource exists when the caller is not authorised to access it.
- Log sufficient diagnostic information for local debugging without logging secrets or unnecessary sensitive data.

## 6. Database integrity and migrations

### BR-08.10 — Use controlled schema migrations
- Use PostgreSQL as the relational database and Flyway for schema migrations.
- Schema changes must be represented by versioned migrations rather than relying on undocumented manual database edits.
- Use database constraints, transactions, and appropriate concurrency controls to support the previously approved integrity and idempotency rules.
- A failed migration must be visible as a startup/deployment failure; the application must not report successful readiness when the required schema is unavailable.

## 7. External-service resilience and safe failure

### BR-08.11 — Preserve the approved FX behavior
- Follow BR-02 for quote expiry, amount and currency validation, rate direction, precision, rounding, and the distinction between external and mock rates.
- If the external FX source fails, use only the approved deterministic mock fallback and label the rate source honestly.
- If neither permitted source can provide a rate, return a controlled error and do not create a fabricated quote.

### BR-08.12 — Preserve the approved mock-provider behavior
- Follow BR-06 for deterministic mock-provider attempts, idempotency, uncertain outcomes, and controlled status inquiry.
- An ambiguous or lost provider response must be represented as `UNKNOWN`, not falsely classified as success or failure.
- Do not create a new provider attempt while the previous attempt is `UNKNOWN`; do not implement blind automatic retries.
- Do not allow a user or Admin to manually fabricate a provider outcome or bypass the approved state machine.

### BR-08.13 — Fail closed for protected workflow decisions
- A configuration, provider, compliance-trigger, or audit failure must not silently clear a compliance hold, bypass dispatch controls, or fabricate a successful transfer.
- If an audit event is required for a state-changing operation and the audit record cannot be persisted, the state change must not commit.
- State changes and their required audit records must be committed atomically where specified in BR-01 through BR-07.

## 8. Input validation and API contracts

### BR-08.14 — Validate all untrusted inputs
- Validate request bodies, path identifiers, query parameters, enumerated values, amounts, currencies, required reasons, and state-dependent actions on the backend.
- Apply the approved amount/currency rules, quote validation, beneficiary ownership and active-status checks, and state-machine constraints.
- Reject malformed or unsupported requests with a stable error code and safe message.
- Do not rely on frontend validation as a security boundary.

### BR-08.15 — Treat server-owned values as authoritative
The backend must not trust client-supplied values for:
- authenticated actor identity or role;
- resource ownership;
- authoritative timestamps;
- transfer state or state-transition outcome;
- exchange rates or calculated destination amounts;
- provider outcomes or compliance decisions.

The server must calculate, load, or validate these values according to the approved business rules.

## 9. Reproducible local development environment

### BR-08.16 — Provide a documented local setup
The prototype baseline is:
- **Backend:** Java 21, Spring Boot, Spring Security.
- **Frontend:** React, TypeScript, Vite.
- **Database:** PostgreSQL with Flyway migrations.
- **Local orchestration:** Docker Compose.
- **Provider simulation:** local deterministic mock provider only.
- **Data:** synthetic demo accounts, beneficiaries, transfers, and compliance triggers.

Provide setup instructions that explain prerequisites, environment configuration, database startup and migration behavior, demo-user setup, application startup, test execution, and how to reset local demo data safely.

The setup must not require real banking credentials, real customer information, or a live payment-provider account.

## 10. Testing and MVP acceptance

### BR-08.17 — Test the approved rules and security boundaries
Automated tests should cover, at minimum:
- authentication required for protected endpoints;
- role-based access and customer ownership isolation;
- invalid input and unsupported state transitions;
- beneficiary and quote validation;
- quote expiry and single-use behavior;
- idempotency and duplicate-request handling;
- compliance-hold dispatch blocking;
- `UNKNOWN` provider outcomes and status inquiry behavior;
- audit/state-change atomicity;
- safe error responses without internal detail leakage;
- migration and local setup behavior where practical.

Include an end-to-end demonstration of the principal synthetic workflow: create a beneficiary, obtain a quote, create and submit a transfer, complete the Operations review, exercise the simulated Compliance hold path where applicable, and observe the deterministic mock-provider result or controlled `UNKNOWN` reconciliation path.

### BR-08.18 — MVP acceptance checklist
The MVP is acceptable when:
- protected endpoints enforce authentication;
- backend role permissions and customer ownership are enforced;
- demo credentials are securely hashed and clearly development-only;
- secrets are not committed to source control;
- request validation and safe error handling are consistent;
- PostgreSQL schema migrations are reproducible through Flyway;
- FX failure follows BR-02 without inventing rates;
- provider uncertainty follows BR-06 without blind retries or fabricated outcomes;
- compliance holds block dispatch;
- required audit records and state changes are atomic;
- the core security and business-rule tests pass;
- a developer can follow the local setup guide using synthetic data;
- the product is clearly labelled as an educational simulation with no real fund movement.

## 11. Edge cases to verify

- An unauthenticated caller invokes a protected endpoint.
- An authenticated user calls an endpoint assigned to another role.
- A customer attempts to access another customer's beneficiary, transfer, or timeline.
- A client submits forged actor, ownership, role, timestamp, rate, amount-calculation, state, or provider-outcome fields.
- Required configuration is missing or malformed.
- A database migration fails or the database is unavailable.
- The external FX source is unavailable and the mock fallback is also unavailable.
- A provider response is lost after an attempt may have been processed.
- A required audit write fails during a state-changing operation.
- An unexpected exception occurs and must not leak a stack trace or sensitive details.
- A user attempts to bypass a hidden frontend control by calling the API directly.
- Demo fixtures or logs accidentally contain real secrets or unnecessary personal data.

## 12. Proposed implementation guidance

The following are implementation recommendations, not additional business permissions:
- Centralise API exception handling so errors follow the stable response contract.
- Use Spring Security for authentication and request-level authorization, supplemented by service-layer ownership and state-transition checks.
- Use Bean Validation or equivalent backend validation for request DTOs.
- Use `BigDecimal` for monetary values and follow BR-02 for precision and rounding.
- Use Flyway versioned migrations and database transactions for workflow changes.
- Use deterministic fixtures and mock-provider behavior to make the demonstration repeatable.
- Keep restricted Compliance details out of customer-facing DTOs and Operations responses.
- Test service-layer rules as well as HTTP-level access controls.

## 13. Explicit exclusions

This specification does not establish:
- production-grade identity verification or account recovery;
- real bank-account verification;
- real sanctions, AML, KYC, or other regulatory screening;
- legal or regulatory compliance certification;
- live payment-provider integration or settlement;
- custody, transfer, disbursement, or movement of real funds;
- production security certification, penetration-test approval, or readiness for real financial operations.

Any future move beyond the educational prototype requires a separate scope, qualified security and compliance review, and appropriate legal, regulatory, and operational assessment.
