---
type: value-object
service: aegis-wallet-service
layer: domain
tags: [ddd, value-object, ledger]
status: implemented
---

# LedgerEntry

Value object representing a single transaction on a wallet's ledger.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `entryId` | UUID | Unique entry ID |
| `type` | [[02 - Domain Models/LedgerEntryType\|LedgerEntryType]] | Credit/debit/reserve/release |
| `amount` | BigDecimal | Transaction amount |
| `currency` | String | ISO 4217 |
| `description` | String | Transaction reference |
| `referenceId` | String | External reference |
| `createdAt` | Instant | Timestamp |

## Used By

- [[02 - Domain Models/Wallet\|Wallet]] aggregate root
- Mapped to: `LedgerEntryJpaEntity` in infrastructure

## JPA Mapping

Stored in `ledger_entries` table, FK to `wallets.id`.
