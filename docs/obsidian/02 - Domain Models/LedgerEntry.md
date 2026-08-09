---
type: value-object
service: aegis-wallet-service
layer: domain
tags: [ddd, value-object, ledger]
status: implemented
---

# LedgerEntry

Immutable value object representing a single transaction on a wallet's ledger. Entries are append-only: corrections and reversals are recorded as new entries referencing the original (ADR-004), never by editing an existing entry.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique entry ID (UUID v7) |
| `walletId` | UUID | The wallet the entry belongs to |
| `type` | [[02 - Domain Models/LedgerEntryType\|LedgerEntryType]] | Entry classification |
| `amount` | BigDecimal | Absolute, non-negative amount |
| `currency` | String | ISO 4217 |
| `reference` | String | External reference (idempotency key for deposits) |
| `timestamp` | Instant | When the entry was created |
| `reversalOf` | UUID | ID of the entry this entry reverses, or `null` |

## Validation

- `amount` must not be negative
- `REVERSAL` entries must reference the original entry (`reversalOf` not null)
- All fields required except `reversalOf`

## Used By

- [[02 - Domain Models/Wallet\|Wallet]] aggregate root
- Mapped to: `LedgerEntryJpaEntity` in infrastructure

## JPA Mapping

Stored in `ledger_entries` table, FK to `wallets.id`.
