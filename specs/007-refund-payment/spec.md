# Feature Specification: UC-007 Refund Payment

**Feature Branch**: `feature/007-refund-payment`

**Created**: 2026-08-13

**Status**: Draft

**Tracked by**: #10 (parent epic #246, core epic #3). Blocks nothing; depends on UC-006 (completed payments).

---

## Problem

A completed payment (UC-006) cannot be reversed. When a transaction is disputed or cancelled, the funds must be returned to the payer's wallet with a proper audit trail. This is the compensation-at-scale use case: reversing a settled payment requires a **new ledger entry (REFUND)**, not an un-settlement.

## Solution

Extend the Payment Service with a **`Refund` aggregate** and a `RefundPaymentUseCase` that:
1. Validates the payment is `COMPLETED` (not already refunded, not failed) and belongs to the requester (or admin override)
2. Validates the refund amount (full or partial) does not exceed the original payment amount
3. Credits the payer's wallet via a **refund credit** (new `REFUND` ledger entry in Wallet Service)
4. Transitions the payment `COMPLETED → REFUNDED`
5. Publishes `PaymentRefunded` (`payment.refunded`) via the outbox
6. Audit Service persists the refund record

### Business Rules

1. Only `COMPLETED` payments can be refunded — `PAYMENT_NOT_REFUNDABLE` otherwise
2. Already-refunded payments → `409 PAYMENT_ALREADY_REFUNDED`
3. Non-existent payments → `404 PAYMENT_NOT_FOUND`
4. Refund amount ≤ original payment amount (partial refunds supported); `amount <= 0` rejected
5. A payer can refund only their own payments; admins can refund any (`X-User-Id` vs payment owner, admin flag)
6. The wallet is **credited** with a `REFUND` ledger entry (reference = refund id) — idempotent by reference
7. `PaymentStatus`: `COMPLETED → REFUNDED`; `RefundStatus`: `PENDING → COMPLETED | FAILED`
8. Events follow the standard envelope (ADR-009) via outbox (ADR-006)
9. Ledger immutability: a refund is a new movement, never a deletion (ADR-004)

---

## User Scenarios & Testing

### User Story 1 - Full refund of a completed payment (Priority: P1)

As a user, I want to refund a completed payment so that my wallet balance is restored.

**Affected Services**: `payment`, `wallet`, `bff`
**Domain Events Published**: `PaymentRefunded`
**API Endpoints**: `POST /api/v1/payments/{id}/refund`, `POST /api/v1/wallets/{id}/holds/{holdId}/debit` (reused), wallet refund-credit endpoint

**Independent Test**: Create a funded wallet, pay 25.00, then refund the payment, assert `REFUNDED`, balance restored (+25.00), `REFUND` ledger entry, `PaymentRefunded` consumed by Audit.

**Acceptance Scenarios**:

1. **Given** a COMPLETED payment of 25.00, **When** the user refunds it, **Then** `200` with `status=COMPLETED` (refund), balance increased by 25.00, `REFUND` ledger entry, `PaymentRefunded` published
2. **Given** a COMPLETED payment, **When** `GET /api/v1/payments/{id}`, **Then** status is `REFUNDED` and the refund is linked

### User Story 2 - Partial refund (Priority: P1)

As a user, I want to refund part of a payment so that I only return what was not used.

**Acceptance Scenarios**:

1. **Given** a COMPLETED payment of 25.00, **When** the user refunds 10.00, **Then** `200`, balance +10.00, payment still `REFUNDED` (once any refund completes, the payment is fully REFUNDED in this model)
2. **Given** a refund amount > 25.00, **When** submitted, **Then** `422 REFUND_EXCEEDS_PAYMENT`

### User Story 3 - Refund validation and conflicts (Priority: P1)

**Acceptance Scenarios**:

1. **Given** a FAILED payment, **When** refunded, **Then** `422 PAYMENT_NOT_REFUNDABLE`
2. **Given** an already-refunded payment, **When** refunded again, **Then** `409 PAYMENT_ALREADY_REFUNDED`
3. **Given** a non-existent payment, **When** refunded, **Then** `404 PAYMENT_NOT_FOUND`
4. **Given** a payment owned by another user, **When** that user refunds it (non-admin), **Then** `403 PAYMENT_NOT_OWNED`
5. **Given** a duplicate refund reference, **When** submitted, **Then** the refund is idempotent (same refund returned, no double credit)

---

### Edge Cases

- **Concurrent refunds**: the payment must be locked (pessimistic) so two concurrent refunds cannot both succeed
- **Partial refund accumulation**: in this model a payment becomes `REFUNDED` on the first successful refund; multiple partial refunds are a future extension (single-refund-per-payment for now)
- **Duplicate event delivery**: consumers deduplicate by `eventId`
- **Amount precision**: `BigDecimal` scale 2, positive; no negative ledger amounts
- **Wallet inactive**: refund credit rejects if the wallet is not ACTIVE (`WALLET_NOT_ACTIVE`)

---

## Requirements

### Functional Requirements

**Payment Service (extension)**

- **FR-001**: System MUST expose `POST /api/v1/payments/{paymentId}/refund` accepting `amount?` (optional, default full), `reason?`, `reference`
- **FR-002**: System MUST persist a `Refund` aggregate with `RefundStatus` `PENDING → COMPLETED | FAILED`, linked to the payment
- **FR-003**: System MUST validate the payment is `COMPLETED` and owned by the requester (admin override), rejecting otherwise
- **FR-004**: System MUST validate refund amount ≤ original payment amount
- **FR-005**: System MUST credit the wallet via the Wallet Service refund-credit endpoint (idempotent by refund reference)
- **FR-006**: System MUST transition the payment `COMPLETED → REFUNDED` and publish `PaymentRefunded` via the outbox
- **FR-007**: System MUST lock the payment during refund (concurrency-safe)

**Wallet Service**

- **FR-008**: System MUST expose a refund-credit endpoint that creates a `REFUND` ledger entry and increases the wallet balance in one transaction (idempotent by reference)

**Fraud / Notification**

- **FR-009**: No fraud re-assessment for refunds (out of scope); Notification Service events produced but service not built (future)

**BFF / Frontend**

- **FR-010**: BFF MUST proxy `POST /api/bff/payments/{id}/refund` to Payment Service
- **FR-011**: Frontend MUST provide a **Refund** action on completed payments in the wallet history

**Cross-cutting**

- **FR-012**: All endpoints MUST authenticate via OAuth2 JWT and return the standard `ErrorResponse`
- **FR-013**: Audit Service MUST consume `PaymentRefunded` and persist an audit record

### Key Entities

- **Refund**: Aggregate in `com.aegis.payment.domain.model`. Attributes: `id` (UUIDv7), `paymentId`, `walletId`, `userId`, `amount`, `currency`, `reason?`, `reference`, `status` (RefundStatus), `createdAt`, `completedAt?`. Published events: `PaymentRefunded`
- **RefundStatus**: Enum — `PENDING`, `COMPLETED`, `FAILED`
- **PaymentRefunded**: Domain event on topic `payment.refunded` (schema in `contracts/events/`)
- **LedgerEntryType.REFUND**: existing wallet enum value (UC-006 pre-defined), now used

---

## API Summary

Contracts in `contracts/api/`:

| Endpoint | Service | Purpose |
|----------|---------|---------|
| `POST /api/v1/payments/{paymentId}/refund` | payment | Refund a completed payment (200 refund / 4xx on failure) |
| `POST /api/bff/payments/{paymentId}/refund` | bff | Session-cookie façade for the SPA |

Error codes: `VALIDATION_ERROR`, `PAYMENT_NOT_FOUND` (404), `PAYMENT_ALREADY_REFUNDED` (409), `PAYMENT_NOT_REFUNDABLE` (422), `PAYMENT_NOT_OWNED` (403), `REFUND_EXCEEDS_PAYMENT` (422), `WALLET_NOT_ACTIVE` (422), `WALLET_NOT_FOUND` (404).

---

## Success Criteria

- **SC-001**: End-to-end refund (BFF → payment → wallet credit → event) completes with p95 < 1.5s
- **SC-002**: Zero balance drift — invariant `balance == SUM(ledger)` verified after every refund, including partial refunds
- **SC-003**: 100% of terminal refunds produce exactly one audit record; duplicate deliveries never duplicate
- **SC-004**: Duplicate refund references never double-credit
- **SC-005**: Domain-layer coverage 100%, overall ≥ 80%; ArchUnit hexagonal checks pass
- **SC-006**: All four test tiers green with reports under `evidence/`
- **SC-007**: OpenAPI and event contracts validated in CI; controllers carry zero swagger annotations

## Assumptions

- **Single refund per payment**: a payment becomes `REFUNDED` after the first successful refund; multiple partial refunds are a future extension
- **Refund is a credit, not a reversal**: `REFUND` ledger entry, immutable (ADR-004)
- **No fraud re-check** on refunds (out of scope)
- **Notification Service not built yet**: `PaymentRefunded` is produced so the future service can consume it
- **Admin override** is a simple flag in the request (RBAC enforcement is future work)

## Sub-Tasks (tracked in GitHub)

- [ ] #10 — Epic UC-007 Refund Payment (this feature)
- [ ] Write spec + contracts + event schemas + ADR-016 (this change)
- [ ] Implement `Refund` domain + `RefundPaymentUseCase` + saga + wallet refund-credit
- [ ] Consumer in Audit Service for `payment.refunded`
- [ ] Tests: unit + integration + E2E + load + evidence
- [ ] BFF proxy + Frontend refund button
