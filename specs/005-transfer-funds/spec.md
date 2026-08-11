# Feature Specification: UC-005 Transfer Funds

**Feature Branch**: `feature/005-transfer-funds`

**Created**: 2026-08-10

**Status**: Draft

**Tracked by**: #8 (parent epic #246, core epic #3). Implementation tasks: #247–#253.

---

## Problem

Users can deposit funds into their wallets, but money cannot move between parties. A digital payment platform whose core promise is moving money currently supports only one-directional funding. Transfers are the first use case that spans multiple bounded contexts (payment, wallet, fraud) and therefore requires a deliberate distributed-consistency design: no distributed transactions, no partial balance updates, no silent failures.

## Solution

Introduce a new **Payment Service** (`aegis-payment-service`, port 8084) that orchestrates peer-to-peer transfers using an **orchestrated saga** (see ADR-014):

1. Authenticated user submits a transfer via BFF (`POST /api/bff/transfers`) → Payment Service (`POST /api/v1/transfers`)
2. Payment Service creates a `Transfer` aggregate (`PENDING`) and publishes `TransferRequested` via the transactional outbox
3. Payment Service calls Fraud Service **synchronously** (`POST /api/v1/fraud/assess`, `transactionType=TRANSFER`)
   - `REJECT` → transfer `FAILED` (`FRAUD_REJECTED`), publish `TransferFailed`
4. On `APPROVE`/`REVIEW`, Payment Service reserves funds in the source wallet (`POST /api/v1/wallets/{id}/holds`) — the hold reduces *available* balance without moving money
5. Payment Service settles atomically via Wallet Service (`POST /api/v1/wallets/transfers/settle`): in a single database transaction the hold is settled, the source wallet is debited (`TRANSFER_OUT` ledger entry) and the destination wallet is credited (`TRANSFER_IN` ledger entry)
6. Transfer transitions to `COMPLETED`; `TransferCompleted` is published via outbox
7. Any failure after reservation triggers compensation: the hold is released and `TransferFailed` is published
8. Audit Service persists the full trail; Reporting Service updates projections

### Business Rules

1. A transfer moves funds between two **distinct, ACTIVE** wallets
2. Fraud check always runs **before** any fund movement; `REJECT` blocks the transfer
3. Funds are reserved (hold) before settlement; available balance = `balance − Σ(active holds)`
4. Debit and credit across the two wallets commit in **one** database transaction (Wallet Service)
5. Ledger entries are immutable and reference the transfer id (idempotency, ADR-004/ADR-008)
6. Client-supplied `reference` is unique per source wallet — duplicates are rejected (`409`)
7. Holds expire (TTL 5 minutes); a scheduled job releases expired holds (orchestrator crash recovery)
8. Currency must match across source wallet, destination wallet and request
9. Events follow the standard envelope (ADR-009) and are published via outbox (ADR-006)

---

## User Scenarios & Testing

### User Story 1 - Successful transfer between two wallets (Priority: P1)

As an authenticated user, I want to transfer funds from my wallet to another user's wallet so that I can send money within the platform.

**Why this priority**: It is the core value proposition of a payment platform and the MVP slice — one happy path end to end proves the saga, the hold mechanism, atomic settlement and the event cascade.

**Affected Services**: `payment` (new), `wallet`, `fraud`, `bff`
**Domain Events Published**: `TransferRequested`, `TransferCompleted`
**API Endpoints**: `POST /api/v1/transfers`, `POST /api/v1/fraud/assess`, `POST /api/v1/wallets/{id}/holds`, `POST /api/v1/wallets/transfers/settle`

**Independent Test**: Create two funded wallets, `POST /api/v1/transfers`, assert `COMPLETED`, balances and ledger entries on both wallets, and `TransferCompleted` consumed by Audit/Reporting.

**Acceptance Scenarios**:

1. **Given** source wallet (ACTIVE, available balance ≥ amount) and destination wallet (ACTIVE), **When** the user submits a transfer, **Then** the response is `201` with `status=COMPLETED`, source balance decreased, destination balance increased, both ledger entries exist, and `TransferCompleted` is published
2. **Given** a completed transfer, **When** `GET /api/v1/transfers/{id}`, **Then** the transfer shows final state, fraud assessment id and timestamps
3. **Given** a completed transfer, **When** Audit/Reporting consume `TransferCompleted`, **Then** an audit record exists and reporting projections reflect the movement

---

### User Story 2 - Transfer rejected by fraud or insufficient funds (Priority: P1)

As the platform, I want to block transfers that fail risk or balance validation so that no unsafe or unfunded movement ever settles.

**Why this priority**: Same priority as the happy path — a payment system that cannot safely say "no" is not viable.

**Acceptance Scenarios**:

1. **Given** fraud assessment returns `REJECT`, **When** a transfer is submitted, **Then** the API returns `422 TRANSFER_REJECTED_BY_FRAUD`, the transfer is `FAILED`, `TransferFailed` is published, and **no hold is created**
2. **Given** available balance < amount, **When** reserving funds, **Then** the API returns `422 INSUFFICIENT_FUNDS`, the transfer is `FAILED`, and no ledger entries are written
3. **Given** fraud service is unavailable (timeout/circuit breaker), **When** a transfer is submitted, **Then** it fails closed: transfer `FAILED` (`FRAUD_UNAVAILABLE`), `503` to the caller, no hold created
4. **Given** same source and destination wallet, **When** submitted, **Then** `400`/`422` validation error, no state change
5. **Given** a non-ACTIVE (suspended/closed) source or destination wallet, **When** submitted, **Then** `422 WALLET_NOT_ACTIVE`

---

### User Story 3 - Compensation when settlement fails (Priority: P2)

As the platform, I want reserved funds released automatically if a transfer cannot complete, so that balances never stay locked by a crashed or failed operation.

**Acceptance Scenarios**:

1. **Given** a hold was created and settlement fails (e.g. destination wallet deactivated mid-flight), **When** the saga compensates, **Then** the hold is `RELEASED`, available balance is restored, transfer is `FAILED`, and `TransferFailed` is published
2. **Given** the Payment Service crashes after creating a hold, **When** the hold TTL expires, **Then** a scheduled job releases the expired hold and the transfer is reconciled to `FAILED`
3. **Given** any `FAILED` transfer, **When** inspecting both wallets, **Then** `balance == SUM(ledger inflows) − SUM(ledger outflows)` holds exactly (no drift)

---

### User Story 4 - Idempotent retry of a transfer submission (Priority: P2)

As a client, I want retries with the same reference to never execute twice, so that network failures cannot duplicate money movement.

**Acceptance Scenarios**:

1. **Given** a completed transfer with reference `R`, **When** the same source wallet submits another transfer with reference `R`, **Then** the API returns `409 TRANSFER_DUPLICATE` and balances are unchanged
2. **Given** concurrent submissions with the same reference, **When** both are processed, **Then** exactly one succeeds

---

### User Story 5 - Transfer history on wallets (Priority: P3)

As a user, I want to see transfers in my wallet's transaction history so that I can track money in and out.

**Acceptance Scenarios**:

1. **Given** completed transfers involving a wallet, **When** listing the wallet's ledger/transactions, **Then** `TRANSFER_OUT`/`TRANSFER_IN` entries appear with amount, counterparty and timestamp
2. **Given** the frontend wallet detail page, **When** a transfer completes, **Then** the history reflects it (via BFF)

---

### Edge Cases

- **Concurrent holds on the same wallet**: available-balance check and hold insertion must be atomic (pessimistic lock on the wallet row); two holds that individually fit must not both succeed if their sum exceeds the balance
- **Deadlock avoidance on settlement**: wallet rows are locked in deterministic order (by wallet id) when debiting/crediting two wallets in one transaction
- **Kafka consumer lag/retry**: `TransferCompleted` consumers are idempotent (ADR-008) and DLT-protected (ADR-010)
- **Duplicate event delivery**: outbox relay may republish after crash — consumers deduplicate by `eventId`
- **Fraud REVIEW decision**: treated as executable for UC-005 (manual review queues are out of scope; see Assumptions)
- **Amount precision**: `BigDecimal` scale 2, positive, `exclusiveMinimum: 0`; no negative ledger amounts (ADR-004)
- **Hold expiry race**: settlement of an expired-but-not-yet-released hold must fail deterministically and trigger compensation
- **Circuit breaker**: sustained fraud/wallet failures open the breaker and fail transfers fast without creating holds

---

## Requirements

### Functional Requirements

**Payment Service (new)**

- **FR-001**: System MUST expose `POST /api/v1/transfers` accepting `sourceWalletId`, `destWalletId`, `amount`, `currency`, `description?`, `reference` (idempotency key)
- **FR-002**: System MUST persist a `Transfer` aggregate with status machine `PENDING → FRAUD_CHECK → FUNDS_RESERVED → COMPLETED | FAILED` (`REVERSED` reserved for UC-007)
- **FR-003**: System MUST call Fraud Service synchronously before reserving funds and MUST fail closed on rejection or unavailability
- **FR-004**: System MUST orchestrate the saga: reserve (hold) → settle → complete, with compensation (release hold) on any post-reservation failure
- **FR-005**: System MUST publish `TransferRequested`, `TransferCompleted`, `TransferFailed` via the transactional outbox (standard envelope, ADR-009)
- **FR-006**: System MUST expose `GET /api/v1/transfers/{transferId}` returning current state, fraud assessment reference and failure reason when present
- **FR-007**: System MUST enforce unique `reference` per source wallet (`409 TRANSFER_DUPLICATE`)
- **FR-008**: System MUST run a scheduled job releasing holds expired beyond their TTL and reconciling their transfers to `FAILED`

**Wallet Service (extension)**

- **FR-009**: System MUST expose `POST /api/v1/wallets/{walletId}/holds` creating an `ACTIVE` hold iff `amount ≤ availableBalance` (atomic check-and-insert under wallet lock)
- **FR-010**: System MUST expose `POST /api/v1/wallets/transfers/settle` performing, in ONE database transaction: hold → `SETTLED`, source debit (`TRANSFER_OUT`), destination credit (`TRANSFER_IN`), balance updates, outbox event(s)
- **FR-011**: System MUST expose `POST /api/v1/wallets/{walletId}/holds/{holdId}/release` marking the hold `RELEASED` (compensation)
- **FR-012**: System MUST compute available balance as `balance − Σ(active holds)` and expose it in wallet detail responses
- **FR-013**: Settlement MUST lock both wallet rows in deterministic (id-ordered) sequence and MUST validate wallet states, currency match, and hold integrity
- **FR-014**: Ledger entries MUST store the transfer reference for idempotency; re-settling the same transfer MUST NOT duplicate entries

**Fraud Service (existing)**

- **FR-015**: Existing `POST /api/v1/fraud/assess` with `transactionType=TRANSFER` is reused unchanged (spec 008)

**BFF / Frontend**

- **FR-016**: BFF MUST proxy `POST /api/bff/transfers` and `GET /api/bff/transfers/{id}` to Payment Service (session cookie → JWT, CSRF protection unchanged)
- **FR-017**: Frontend MUST provide a transfer form (destination wallet, amount, description) from the wallet detail page and surface transfer entries in wallet history (#253)

**Cross-cutting**

- **FR-018**: All endpoints MUST authenticate via OAuth2 JWT and return the standard `ErrorResponse` (`code`, `message`, `details`, `timestamp`)
- **FR-019**: All services MUST emit OpenTelemetry traces spanning the saga (BFF → payment → fraud → wallet) per ADR-012
- **FR-020**: Audit Service MUST consume all three transfer events and persist immutable audit records; Reporting Service MUST consume `TransferCompleted`/`TransferFailed` and update projections

### Key Entities

- **Transfer**: Aggregate in `com.aegis.payment.domain.model`. Attributes: `id` (UUIDv7), `sourceWalletId`, `destWalletId`, `amount` (Money VO), `currency`, `description?`, `reference`, `status` (TransferStatus), `fraudAssessmentId?`, `holdId?`, `failureReason?`, `createdAt`, `updatedAt`, `completedAt?`. Published events: `TransferRequested`, `TransferCompleted`, `TransferFailed`
- **TransferStatus**: Enum — `PENDING`, `FRAUD_CHECK`, `FUNDS_RESERVED`, `COMPLETED`, `FAILED`, `REVERSED`
- **Hold**: Entity in Wallet Service — `id`, `walletId`, `amount`, `currency`, `reference` (= transfer id), `status` (`ACTIVE`, `SETTLED`, `RELEASED`, `EXPIRED`), `createdAt`, `expiresAt`
- **LedgerEntry** (existing): new uses of existing types `TRANSFER_OUT` / `TRANSFER_IN`, `reference` = transfer id
- **TransferRequested / TransferCompleted / TransferFailed**: Domain events on topics `payment.transfer.requested|completed|failed` (schemas in `contracts/events/`)

---

## API Summary

Contracts in `contracts/api/`:

| Endpoint | Service | Purpose |
|----------|---------|---------|
| `POST /api/v1/transfers` | payment | Initiate transfer (201 `COMPLETED` / 4xx on failure) |
| `GET /api/v1/transfers/{id}` | payment | Transfer status and detail |
| `POST /api/v1/wallets/{id}/holds` | wallet | Reserve funds (internal, called by payment) |
| `POST /api/v1/wallets/transfers/settle` | wallet | Atomic debit+credit (internal, called by payment) |
| `POST /api/v1/wallets/{id}/holds/{holdId}/release` | wallet | Compensation (internal, called by payment) |
| `POST /api/bff/transfers`, `GET /api/bff/transfers/{id}` | bff | Session-cookie façade for the SPA |

Error codes: `VALIDATION_ERROR`, `TRANSFER_DUPLICATE` (409), `WALLET_NOT_FOUND` (404), `INSUFFICIENT_FUNDS` (422), `WALLET_NOT_ACTIVE` (422), `CURRENCY_MISMATCH` (422), `SELF_TRANSFER` (422), `TRANSFER_REJECTED_BY_FRAUD` (422), `FRAUD_UNAVAILABLE` (503), `TRANSFER_NOT_FOUND` (404), `HOLD_NOT_FOUND` (404), `HOLD_NOT_ACTIVE` (409).

---

## Success Criteria

- **SC-001**: End-to-end transfer (BFF → payment → fraud → wallet → events) completes with p95 < 2s under the standard k6 load profile; fraud assessment within its 200ms budget
- **SC-002**: Zero balance drift — invariant `balance == SUM(ledger)` verified on both wallets after every integration/E2E transfer, including compensated failures
- **SC-003**: 100% of terminal transfers (`COMPLETED`/`FAILED`) produce exactly one audit record; duplicate event deliveries never duplicate audit rows
- **SC-004**: Duplicate `reference` submissions never double-execute (0 duplicates across the concurrency test suite)
- **SC-005**: Domain-layer coverage 100%, overall ≥ 80% (constitution §V); ArchUnit hexagonal checks pass for the new module
- **SC-006**: All four test tiers green with reports under `evidence/` (unit, integration, E2E, load)
- **SC-007**: OpenAPI and event contracts validated in CI; controllers carry zero swagger annotations (spec-first)

## Assumptions

- **New service**: `aegis-payment-service` is created as a new Maven module (port 8084, PostgreSQL `aegis_payment`) following the standard hexagonal scaffold — this grows the service catalog to 7 backend services (catalog, project-status and portfolio must be updated in the same change)
- **Synchronous orchestration**: fraud and wallet steps are synchronous REST calls from the Payment Service saga orchestrator; Kafka carries facts (events), not commands (ADR-014)
- **REVIEW = proceed**: fraud `REVIEW` decisions execute the transfer in UC-005; manual review queues are deferred to a future use case
- **Single currency per transfer**: cross-currency/FX is out of scope
- **Same-tenant wallets**: both wallets live in the same Wallet Service instance/DB — atomic settlement in one transaction is possible (this is what makes the hold+settle design safe without 2PC)
- **Notifications out of scope**: the future Notification Service will consume `TransferCompleted`/`TransferFailed`; UC-005 only guarantees the events exist
- **Internal wallet endpoints**: holds/settle/release are service-to-service endpoints, not exposed through the BFF
- **Existing conventions apply**: idempotency (ADR-008), event envelope (ADR-009), retry/DLT (ADR-010), outbox (ADR-006), ledger immutability (ADR-004)

## Sub-Tasks (tracked in GitHub)

- [ ] #247 — Spec, OpenAPI contracts, event schemas, ADR-014 (this change)
- [ ] #248 — Scaffold `aegis-payment-service` (module, domain, ports, docker-compose, Helm, catalog)
- [ ] #249 — Wallet Service: holds, available balance, atomic settle, release
- [ ] #250 — Fraud integration (sync assess, fail-closed, circuit breaker)
- [ ] #251 — Events + consumers (audit, reporting)
- [ ] #252 — Tests all tiers + evidence
- [ ] #253 — Frontend transfer form + wallet history
