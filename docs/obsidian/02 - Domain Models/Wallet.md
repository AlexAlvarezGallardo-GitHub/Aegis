---
type: domain-model
service: aegis-wallet-service
layer: domain
tags: [ddd, aggregate, wallet]
status: implemented
---

# Wallet

Aggregate root for the Wallet bounded context.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `walletId` | [[02 - Domain Models/WalletId\|WalletId]] | Unique identifier |
| `userId` | UUID | Owner's identity |
| `balance` | BigDecimal | Current balance |
| `currency` | String | ISO 4217 currency code |
| `status` | [[02 - Domain Models/WalletStatus\|WalletStatus]] | Wallet state |
| `createdAt` | Instant | Creation timestamp |
| `updatedAt` | Instant | Last update timestamp |
| `version` | long | Optimistic locking version |
| `ledgerEntries` | List<[[02 - Domain Models/LedgerEntry\|LedgerEntry]]> | Append-only transaction history |

## Factory Methods

- `Wallet.create(userId, currency)` → Wallet with ACTIVE status, zero balance, and an OPENING ledger entry
- `Wallet.rehydrate(walletId, userId, balance, currency, status, createdAt, updatedAt, version, ledgerEntries)` → reconstituted aggregate

## Business Methods

- `adjustBalance(amount, description)` → DEPOSIT (positive) or WITHDRAWAL (negative) entry; throws `InsufficientFundsException` if the balance would go negative
- `depositFunds(amount, source, reference, description)` → DEPOSIT entry with idempotency reference
- `deactivate(targetStatus)` → sets FROZEN or CLOSED (target cannot be ACTIVE, requires balance = 0)
- `reverseDeposit(depositEntryId, reference, description)` → appends an immutable REVERSAL entry referencing the original DEPOSIT (ADR-004); throws `DepositReversalException`
- `isPremium()` → true if currency is EUR and balance > 1000

## Domain Events Published

- [[03 - Domain Events/WalletCreated\|WalletCreated]] (on create)
- [[03 - Domain Events/WalletBalanceAdjusted\|WalletBalanceAdjusted]] (on adjustBalance)
- [[03 - Domain Events/FundsDeposited\|FundsDeposited]] (on depositFunds)

## Relationships

- Contains: [[02 - Domain Models/WalletId\|WalletId]], [[02 - Domain Models/WalletStatus\|WalletStatus]], [[02 - Domain Models/LedgerEntry\|LedgerEntry]]
- References: [[02 - Domain Models/User\|User]] via `userId`
- Consumed by: [[04 - Ports/outbound/WalletRepository\|WalletRepository]]
- Mapped to: `WalletJpaEntity` in infrastructure

## Business Rules

- Balance uses BigDecimal (no floating-point errors)
- Max 5 wallets per user (configurable `aegis.wallet.max-per-user`)
- Deactivation requires balance = €0.00
- Ledger is append-only: corrections and reversals are recorded as new entries (ADR-004)
