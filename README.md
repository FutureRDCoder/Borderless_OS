# BorderlessOS

Cross-border remittance and compliance-operations **educational prototype**.

> BorderlessOS is a simulation only. It does not move, custody, disburse, or settle real funds. All provider outcomes and compliance triggers are synthetic and deterministic.

## Stack

- **Frontend:** React 19, TypeScript, Vite, React Router
- **Backend:** Java 21, Spring Boot 4, Spring Security, Spring Data JPA
- **Database:** PostgreSQL 17 with Flyway migrations
- **Runtime:** Docker Compose (PostgreSQL + backend + nginx-served frontend)
- **Tests:** JUnit, MockMvc, Testcontainers PostgreSQL

## Quick start

Prerequisites: Docker with Compose v2.

```bash
cp .env.example .env
# Replace DB_PASSWORD in .env with a strong local password.

docker compose up --build
```

Open <http://localhost:3000>.

The backend health endpoint is <http://localhost:8080/actuator/health>.

To stop the stack while preserving database data:

```bash
docker compose down
```

To stop and **delete all transfer data**, use `docker compose down -v`. The `-v` flag removes the PostgreSQL volume and cannot be undone.

## Demo accounts

All seeded demo accounts use the password **`BorderlessDemo123!`** unless the corresponding `DEMO_*_PASSWORD` environment variable is set.

| Role | Email | Primary workspace |
|---|---|---|
| Customer | `customer@borderlessos.local` | Beneficiaries, FX quote, draft/submit transfer |
| Operations | `operations@borderlessos.local` | Review queue, request info, reject, mark ready, mock dispatch/inquiry |
| Compliance | `compliance@borderlessos.local` | Active holds, maintain/clear decisions |
| Admin | `admin@borderlessos.local` | Read-only overview, users, transfers, audit events |

The login screen provides a **Demo role** drop-down that prefills the selected account's work email and password without displaying the password as text. Local demo credentials are documented in `DEMO_ACCOUNTS.local.md`, which is excluded from version control. New customers can create their own account from the **Create an account** link; self-signup always receives the `CUSTOMER` role and signs in automatically.

## Demo workflow

1. Sign in as **Customer**.
2. Add a beneficiary. Use a supported corridor, for example `US` / `USD`, `GB` / `GBP`, `JP` / `JPY`, `AU` / `AUD`, or `NZ` / `NZD`.
3. Request an FX quote and create a draft transfer.
4. Submit the transfer. Submission requires an `Idempotency-Key`; the UI sends one automatically.
5. Sign in as **Operations** and review the transfer.
6. Optionally request missing information, sign back in as Customer to respond, and return to Operations.
7. Mark the transfer ready and start the mock-provider attempt.

Deterministic provider scenarios:

| Destination | Scenario | Result |
|---|---|---|
| `US` | `SUCCESS` | `SUCCEEDED` |
| `GB` | `FAILURE` | `FAILED` |
| `JP` | `TIMEOUT_RESOLVES_SUCCESS` | `UNKNOWN`, then inquiry resolves to `SUCCEEDED` |
| `AU` | `TIMEOUT_INCONCLUSIVE` | `UNKNOWN`, inquiry stays inconclusive |
| `NZ` | `SUCCESS` | Automatic compliance hold, then `SUCCEEDED` after Compliance clears it |

The `NZ` corridor is held by the illustrative `SIM-DESTINATION-REVIEW` compliance trigger. Only a **Compliance** user can clear the hold, with a mandatory reason. Admin cannot clear holds or override workflow state.

## Implemented workflow

```text
DRAFT → SUBMITTED → UNDER_REVIEW
UNDER_REVIEW → NEEDS_INFO → SUBMITTED → UNDER_REVIEW
UNDER_REVIEW → COMPLIANCE_HOLD → UNDER_REVIEW (Compliance only)
UNDER_REVIEW → READY_FOR_PROVIDER → PROCESSING
PROCESSING → SUCCEEDED | FAILED | UNKNOWN
UNKNOWN → SUCCEEDED | FAILED (status inquiry only)
UNDER_REVIEW → REJECTED
```

Key invariants enforced by the backend:

- Active compliance holds block readiness and dispatch.
- Only Compliance can maintain or clear holds, with a non-blank reason.
- Admin has read-only oversight and no business-outcome authority.
- FX quotes are single-use and expire after five minutes.
- Submission is idempotent and retained for at least 24 hours.
- Provider timeouts become `UNKNOWN`, never failure; no automatic retry or second attempt.
- Audit history is append-only from the application's perspective and commits with each state change.
- Customers can only access their own transfers and beneficiaries.

## Project layout

```text
.
├── backend/borderlessos-backend/   Spring Boot API, Flyway migrations, tests
├── frontend/                       React + TypeScript SPA and nginx config
├── docs/                           Approved business rules and workflow documents
├── compose.yaml                    PostgreSQL + backend + frontend
└── .env.example                    Local environment template
```

## Local development

Backend:

```bash
cd backend/borderlessos-backend
docker compose up -d                 # local PostgreSQL
set -a && source .env && set +a      # export DB_PASSWORD
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Vite serves on <http://localhost:5173> and proxies `/api` to the backend on port 8080.

## Verification

```bash
cd backend/borderlessos-backend
./mvnw test                          # 64 tests, Testcontainers PostgreSQL

cd frontend
npm run lint
npm run build
```

The backend test suite covers customer signup and session establishment, beneficiary/FX APIs, RBAC, the full transfer workflow, idempotency conflicts, request-info/rejection paths, compliance hold clearance, deterministic provider outcomes and inquiries, invalid transitions, and ownership boundaries.

## API overview

All endpoints are under `/api/v1`. The backend uses server-side session cookies and CSRF protection.

| Area | Representative endpoints |
|---|---|
| Auth | `GET /auth/csrf`, `POST /auth/login`, `POST /auth/signup`, `POST /auth/logout`, `GET /auth/me` |
| Beneficiaries | `GET/POST/PATCH/DELETE /beneficiaries` |
| FX | `POST /fx/quotes`, `GET /fx/quotes/{id}` |
| Customer transfers | `GET/POST /transfers`, `PATCH /transfers/{id}`, `POST /transfers/{id}/submit`, `POST /transfers/{id}/respond`, `GET /transfers/{id}/timeline` |
| Operations | `GET /operations/queue`, `POST /operations/transfers/{id}/request-info`, `reject`, `mark-ready`, `dispatch`, `provider-status-inquiry` |
| Compliance | `GET /compliance/queue`, `GET /compliance/review-queue`, `POST /compliance/transfers/{id}/place-hold`, `maintain-hold`, `clear-hold` |
| Admin | `GET /admin/dashboard`, `GET /admin/users`, `GET /admin/transfers`, `GET /admin/audit-events` |

Errors follow a consistent shape with `status`, `code`, `message`, `path`, `requestId`, and optional `validationErrors`.

## Limitations

- Simulated payments and compliance decisions only; no real screening, account verification, or money movement.
- Local mock provider with deterministic per-corridor outcomes.
- Frankfurter FX is the primary rate source with a local deterministic fallback.
- No customer cancellation after submission and no retry-after-failure flow, by design.
- No document upload or production-grade anti-fraud/identity capability.
