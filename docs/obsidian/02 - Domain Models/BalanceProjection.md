---
type: domain-model
service: aegis-reporting-service
layer: domain
tags: [ddd, record, reporting, read-model]
status: implemented
---

# BalanceProjection

Read-model record holding the latest known balance for a wallet, updated whenever a funds deposit event is consumed from Kafka.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier (UUID v7) |
| `walletId` | UUID | Wallet identifier |
| `userId` | UUID | User identifier |
| `balance` | BigDecimal | Current projected balance |
| `currency` | String | ISO 4217 currency code |
| `lastUpdated` | Instant | Timestamp of the last balance update |

## Factory Methods

- `BalanceProjection.create(walletId, userId, balance, currency, lastUpdated)` → new projection
- `withUpdatedBalance(newBalance, updatedAt)` → new projection with updated balance and timestamp (immutable)

## Source Events

- [[03 - Domain Events/FundsDeposited|FundsDeposited]] consumed from `wallet.funds.deposited`

## Relationships

- Consumed by: [[04 - Ports/outbound/BalanceProjectionRepository|BalanceProjectionRepository]]
