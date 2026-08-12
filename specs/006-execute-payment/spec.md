# Feature Specification: UC-006 Execute Payment

**Feature Branch**: `feature/006-execute-payment`

**Created**: 2026-08-12

**Status**: Draft

**Tracked by**: #9 (parent epic #246, core epic #3).

---

## Problem

Users can transfer funds between wallets (UC-005) but cannot pay an external party — a merchant or payee — from their wallet balance. A digital payment platform must let the user spend wallet funds with a merchant, with the same integrity guarantees as transfers: fraud assessed before execution, funds reserved during processing, atomic debit on success, and compensation (hold release) on failure.

## Solution

Extend the Payment Service with a **Payment aggregate** and a new `ExecutePaymentUseCase` that reuses the proven UC-005 saga mechanics (fraud assessment → wallet hold → atomic settle, ADR-014). The payment flow:

1. Authenticated user submits a payment via BFF (`POST /api/bff/payments`) → Payment Service (`POST /api/v1/payments`)
2. Payment Service creates a `Payment` aggregate (`PENDING`) and publishes `PaymentRequested` via the transactional outbox
3. Fraud Service assesses synchronously (`POST /api/v1/fraud/assess`, `transactionType=PAYMENT`) — `REJECT`/unavailable fails closed
4. On `APPROVE`/`REVIEW`, Wallet Service reserves funds (`POST /api/v1/wallets/{id}/holds`) — payment → `PROCESSING`
5. Payment executes against the payee (idempotent settlement): Wallet Service debits atomically (`POST /api/v1/wallets/transfers/settle`), payment → `COMPLETED`, publish `PaymentExecuted`
6. Any failure after reservation releases the hold (compensation), payment → `FAILED`, publish `PaymentFailed`
7. Audit Service persists the full payment trail; `REFUNDED` reserved for UC-007

### Business Rules

1. A payment debits a **single ACTIVE** wallet owned by the authenticated user
2. Fraud check always runs **before** any fund movement; `REJECT` blocks the payment
3. Funds are reserved (hold) during processing; available balance = `balance − Σ(active holds)`
4. Settlement debits the wallet in **one** database transaction (Wallet Service), keyed by the payment reference (idempotency)
5. Ledger entries are immutable and reference the payment id (ADR-004/ADR-008)
6. Client-supplied `reference` is unique per wallet — duplicates are rejected (`409`)
7. Holds expire (TTL 5 minutes); a scheduled job releases orphaned holds (orchestrator crash recovery)
8. Events follow the standard envelope (ADR-009) via outbox (ADR-006)
9. `PaymentStatus`: `PENDING → PROCESSING → COMPLETED | FAILED`; `REFUNDED` is a terminal state reached from `COMPLETED` in UC-007

---

## User Scenarios & Testing

### User Story 1 - Successful payment to a merchant (Priority: P1)

As an authenticated user, I want to pay a merchant from my wallet balance so that I can make purchases within the platform.

**Affected Services**: `payment`, `wallet`, `fraud`, `bff`
**Domain Events Published**: `PaymentRequested`, `PaymentExecuted`
**API Endpoints**: `POST /api/v1/payments`, `POST /api/v1/fraud/assess`, `POST /api/v1/wallets/{id}/holds`, `POST /api/v1/wallets/transfers/settle`

**Independent Test**: Create a funded wallet, `POST /api/v1/payments`, assert `COMPLETED`, balance decreased, `TRANSFER_OUT`-style ledger entry with the payee reference, and `PaymentExecuted` consumed by Audit.

**Acceptance Scenarios**:

1. **Given** an ACTIVE funded wallet, **When** the user submits a payment, **Then** the response is `201` with `status=COMPLETED`, balance decreased by the amount, ledger entry exists, and `PaymentExecuted` is published
2. **Given** a completed payment, **When** `GET /api/v1/payments/{id}`, **Then** the payment shows final state, payee and timestamps
3. **Given** a completed payment, **When** Audit consumes `PaymentExecuted`, **Then** an audit record exists

### User Story 2 - Payment rejected by fraud or insufficient funds (Priority: P1)

As the platform, I want to block payments that fail risk or balance validation so that no unsafe or unfunded payment ever settles.

**Acceptance Scenarios**:

1. **Given** fraud returns `REJECT`, **When** a payment is submitted, **Then** `422 PAYMENT_REJECTED_BY_FRAUD`, payment `FAILED`, `PaymentFailed` published, **no hold created**
2. **Given** available balance < amount, **When** reserving funds, **Then** `422 INSUFFICIENT_FUNDS`, payment `FAILED`, no ledger entries
3. **Given** fraud service unavailable, **When** submitting, **Then** fails closed: `503 FRAUD_UNAVAILABLE`, payment `FAILED`, no hold
4. **Given** a non-ACTIVE wallet, **When** submitting, **Then** `422 WALLET_NOT_ACTIVE`

### User Story 3 - Compensation when settlement fails (Priority: P2)

As the platform, I want reserved funds released automatically if a payment cannot complete, so balances never stay locked.

**Acceptance Scenarios**:

1. **Given** a hold was created and settlement fails, **When** the saga compensates, **Then** the hold is `RELEASED`, payment `FAILED`, `PaymentFailed` published
2. **Given** the Payment Service crashes after creating a hold, **When** the hold TTL expires, **Then** the scheduled job releases it and the payment reconciles to `FAILED`

### User Story 4 - Idempotent retry of a payment (Priority: P2)

As a client, I want retries with the same reference to never execute twice.

**Acceptance Scenarios**:

1. **Given** a completed payment with reference `R`, **When** the same wallet submits a payment with reference `R`, **Then** `409 PAYMENT_DUPLICATE`, balances unchanged
2. **Given** concurrent submissions with the same reference, **When** both are processed, **Then** exactly one succeeds

### User Story 5 - Payment history on wallets (Priority: P3)

As a user, I want to see payments in my wallet's transaction history.

**Acceptance Scenarios**:

1. **Given** completed payments involving a wallet, **When** listing the wallet's ledger, **Then** payment entries appear with payee and timestamp
2. **Given** the frontend wallet detail page, **When** a payment completes, **Then** the history reflects it (via BFF)

---

### Edge Cases

- **Concurrent holds**: available-balance check and hold insertion atomic under wallet lock
- **Deadlock avoidance**: deterministic id-ordered wallet lock (single wallet — no cross-wallet ordering needed)
- **Duplicate event delivery**: consumers deduplicate by `eventId`
- **Fraud REVIEW**: treated as executable (manual review queues out of scope)
- **Amount precision**: `BigDecimal` scale 2, positive; no negative ledger amounts
- **Hold expiry race**: settlement of an expired hold fails deterministically → compensation
- **Circuit breaker**: sustained fraud/wallet failures fail payments fast without creating holds

---

## Requirements

### Functional Requirements

**Payment Service (extension)**

- **FR-001**: System MUST expose `POST /api/v1/payments` accepting `walletId`, `amount`, `currency`, `payee` (name, id, type), `description?`, `reference`
- **FR-002**: System MUST persist a `Payment` aggregate with status machine `PENDING → PROCESSING → COMPLETED | FAILED` (`REFUNDED` reserved for UC-007)
- **FR-003**: System MUST call Fraud Service synchronously before reserving funds, fail-closed on rejection/unavailability
- **FR-004**: System MUST orchestrate: reserve (hold) → settle → complete, with compensation (release hold) on any post-reservation failure
- **FR-005**: System MUST publish `PaymentRequested`, `PaymentExecuted`, `PaymentFailed` via the transactional outbox (standard envelope)
- **FR-006**: System MUST expose `GET /api/v1/payments/{paymentId}` returning current state, payee and failure reason
- **FR-007**: System MUST enforce unique `reference` per wallet (`409 PAYMENT_DUPLICATE`)
- **FR-008**: System MUST run the scheduled job releasing expired holds and reconciling payments to `FAILED`

**Wallet Service**

- **FR-009**: Existing holds/settle/release endpoints are reused unchanged (UC-005) — the settle call debits the payer wallet; `reference` = payment id

**Fraud Service**

- **FR-010**: Existing `POST /api/v1/fraud/assess` with `transactionType=PAYMENT` is reused unchanged

**BFF / Frontend**

- **FR-011**: BFF MUST proxy `POST /api/bff/payments` and `GET /api/bff/payments/{id}` to Payment Service (session cookie → JWT, CSRF unchanged)
- **FR-012**: Frontend MUST provide a payment form (payee, amount, description) and surface payments in wallet history

**Cross-cutting**

- **FR-013**: All endpoints MUST authenticate via OAuth2 JWT and return the standard `ErrorResponse`
- **FR-014**: All services MUST emit OpenTelemetry traces spanning the saga (ADR-012)
- **FR-015**: Audit Service MUST consume the three payment events and persist immutable audit records

### Key Entities

- **Payment**: Aggregate in `com.aegis.payment.domain.model`. Attributes: `id` (UUIDv7), `walletId`, `userId`, `amount` (Money VO), `currency`, `payee` (Payee VO), `description?`, `reference`, `status` (PaymentStatus), `fraudAssessmentId?`, `holdId?`, `failureReason?`, `createdAt`, `updatedAt`, `completedAt?`. Published events: `PaymentRequested`, `PaymentExecuted`, `PaymentFailed`
- **PaymentStatus**: Enum — `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `REFUNDED`
- **Payee**: Value object — `name`, `id`, `type` (`MERCHANT` | `INDIVIDUAL` | `SERVICE`)
- **PaymentRequested / PaymentExecuted / PaymentFailed**: Domain events on topics `payment.requested|executed|failed` (schemas in `contracts/events/`)

---

## API Summary

Contracts in `contracts/api/`:

| Endpoint | Service | Purpose |
|----------|---------|---------|
| `POST /api/v1/payments` | payment | Initiate payment (201 `COMPLETED` / 4xx on failure) |
| `GET /api/v1/payments/{id}` | payment | Payment status and detail |
| `POST /api/bff/payments`, `GET /api/bff/payments/{id}` | bff | Session-cookie façade for the SPA |

Error codes: `VALIDATION_ERROR`, `PAYMENT_DUPLICATE` (409), `WALLET_NOT_FOUND` (404), `INSUFFICIENT_FUNDS` (422), `WALLET_NOT_ACTIVE` (422), `CURRENCY_MISMATCH` (422), `PAYMENT_REJECTED_BY_FRAUD` (422), `FRAUD_UNAVAILABLE` (503), `PAYMENT_NOT_FOUND` (404), `HOLD_NOT_FOUND` (404), `HOLD_NOT_ACTIVE` (409).

---

## Success Criteria

- **SC-001**: End-to-end payment (BFF → payment → fraud → wallet → events) completes with p95 < 1.5s under the standard k6 profile
- **SC-002**: Zero balance drift — invariant `balance == SUM(ledger)` verified after every integration/E2E payment, including compensated failures
- **SC-003**: 100% of terminal payments produce exactly one audit record; duplicate deliveries never duplicate audit rows
- **SC-004**: Duplicate `reference` submissions never double-execute
- **SC-005**: Domain-layer coverage 100%, overall ≥ 80%; ArchUnit hexagonal checks pass
- **SC-006**: All four test tiers green with reports under `evidence/`
- **SC-007**: OpenAPI and event contracts validated in CI; controllers carry zero swagger annotations

## Assumptions

- **Reuses UC-005 mechanics**: the hold/settle/release wallet endpoints, outbox, fraud gateway and saga compensation pattern are reused unchanged — this is the point of ADR-014
- **Single-wallet payment**: a payment debits one wallet (no cross-wallet debit needed)
- **Merchant settlement is internal**: the "payee execution" is modelled as an atomic wallet debit (the ledger entry records the payee); external PSP/acquiring integration is out of scope
- **Notification Service not yet built**: the events exist so the future Notification Service can consume them; UC-006 does not build it
- **Same conventions apply**: idempotency (ADR-008), event envelope (ADR-009), retry/DLT (ADR-010), outbox (ADR-006), ledger immutability (ADR-004)

## Sub-Tasks (tracked in GitHub)

- [ ] #9 — Epic UC-006 Execute Payment (this feature; sub-tasks below are implementation tracking)
- [ ] Write spec + contracts + event schemas + ADR (this change)
- [ ] Implement `Payment` domain + `ExecutePaymentUseCase` + saga in Payment Service
- [ ] Consumers in Audit Service for `payment.*`
- [ ] Tests: unit + integration + E2E + load + evidence
- [ ] BFF proxy + Frontend payment form
