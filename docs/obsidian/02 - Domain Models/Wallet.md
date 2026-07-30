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
| `name` | String | Display name (nullable) |
| `currency` | String | ISO 4217 currency code |
| `balance` | BigDecimal | Current balance |
| `reservedBalance` | BigDecimal | Locked/reserved amount |
| `status` | [[02 - Domain Models/WalletStatus\|WalletStatus]] | Wallet state |
| `ledgerEntries` | List<[[02 - Domain Models/LedgerEntry\|LedgerEntry]]> | Transaction history |
| `createdAt` | Instant | Creation timestamp |

## Factory Methods

- `Wallet.create(userId, currency)` → Wallet with ACTIVE status, zero balance

## Business Methods

- `updateName(name)` → changes display name
- `deactivate()` → sets CLOSED (validates balance=0 + other active wallets)
- `reactivate()` → sets ACTIVE (from CLOSED or FROZEN)
- `freeze()` → sets FROZEN

## Domain Events Published

- [[03 - Domain Events/WalletCreated\|WalletCreated]] (on create)
- [[03 - Domain Events/WalletUpdated\|WalletUpdated]] (on name update)
- [[03 - Domain Events/WalletDeactivated\|WalletDeactivated]] (on deactivate)
- [[03 - Domain Events/WalletReactivated\|WalletReactivated]] (on reactivate)

## Relationships

- Contains: [[02 - Domain Models/WalletId\|WalletId]], [[02 - Domain Models/WalletStatus\|WalletStatus]], [[02 - Domain Models/LedgerEntry\|LedgerEntry]]
- References: [[02 - Domain Models/User\|User]] via `userId`
- Consumed by: [[04 - Ports/outbound/WalletRepository\|WalletRepository]]
- Mapped to: `WalletJpaEntity` in infrastructure

## Business Rules

- Balance uses BigDecimal (no floating-point errors)
- Max 5 wallets per user
- Deactivation requires balance = €0.00 AND ≥1 other active wallet
