# Feature Specification: UC-004 Deposit Funds

**Feature Branch**: `feature/004-deposit-funds`

**Created**: 2026-07-30

**Status**: Draft

---

## Problem

Users need to deposit funds into their wallets from external sources (bank transfer, card, etc.) with proper tracking, idempotency, and auditability. The existing `PATCH /balance` endpoint is generic and doesn't provide source tracking, idempotency guarantees, or dedicated deposit events.

## Solution

Add a dedicated **Deposit Funds** endpoint that:
- Accepts deposit requests with amount, currency, source, and external reference
- Validates wallet is ACTIVE and amount is positive
- Creates a ledger entry of type DEPOSIT with source/reference tracking
- Publishes a `FundsDeposited` domain event for downstream services
- Rejects duplicate deposit requests (same reference) gracefully

### Business Rules
1. **Amount must be positive** — Zero or negative amounts are rejected
2. **Wallet must be ACTIVE** — Frozen/closed wallets cannot receive deposits
3. **Idempotency by reference** — The same external reference cannot be processed twice
4. **Source tracking** — Source must be specified (BANK_TRANSFER, CARD, etc.)

---

## Affected Services

| Service | Role |
|---------|------|
| **aegis-wallet-service** | New deposit endpoint + domain logic + FundsDeposited event |
| **aegis-bff-service** | Proxy new deposit endpoint |
| **aegis-reporting-service** | ⏳ Consume FundsDeposited (future) |
| **aegis-audit-service** | ⏳ Consume FundsDeposited (future) |

---

## Architecture

```
Client ──→ BFF ──→ Wallet Service ──→ PostgreSQL
                              │
                              └──→ Kafka: wallet.funds.deposited
                                        │
                                        ├──→ Reporting Service (future)
                                        └──→ Audit Service (future)
```

---

## API

### POST /api/v1/wallets/{walletId}/deposits

Deposits funds into the specified wallet.

**Request**:
```json
{
  "amount": 100.00,
  "currency": "EUR",
  "source": "BANK_TRANSFER",
  "reference": "TXN-123456"
}
```

**Response (201)**:
```json
{
  "depositId": "uuid",
  "walletId": "uuid",
  "newBalance": 100.00,
  "amount": 100.00,
  "currency": "EUR",
  "source": "BANK_TRANSFER",
  "reference": "TXN-123456",
  "timestamp": "2026-07-30T12:00:00Z"
}
```

### Error: Duplicate reference (409)
```json
{
  "code": "DUPLICATE_DEPOSIT",
  "message": "Duplicate deposit request with reference: TXN-123456",
  "details": null,
  "timestamp": "2026-07-30T12:00:00Z"
}
```

---

## Domain Model Changes

### Wallet — New Method
- `depositFunds(BigDecimal amount, String source, String reference, String description)` — Validates wallet is ACTIVE and amount > 0, updates balance, creates DEPOSIT ledger entry with source/reference.

---

## Domain Events

### FundsDeposited
New event published when a deposit is processed:
- `eventId: UUID`
- `eventType: String` (FUNDS_DEPOSITED)
- `schemaVersion: String` (1.0)
- `walletId: UUID`
- `userId: UUID`
- `amount: BigDecimal`
- `currency: String`
- `source: String`
- `reference: String`
- `newBalance: BigDecimal`
- `timestamp: Instant`
- `correlationId: String`

Kafka topic: `wallet.funds.deposited`

---

## Sub-Tasks

- [x] Create FundsDeposited domain event
- [x] Create DuplicateDepositException
- [x] Add depositFunds method to Wallet.java
- [x] Create DepositFundsUseCase inbound port
- [x] Create DepositFundsCommand DTO
- [x] Create DepositReceipt DTO
- [x] Create DepositFundsService application service
- [x] Update EventPublisher + KafkaEventPublisher
- [x] Add POST /api/v1/wallets/{walletId}/deposits endpoint
- [x] Add DuplicateDepositException handler
- [x] Add BFF proxy endpoint
- [ ] Write unit tests
- [ ] Write integration tests
