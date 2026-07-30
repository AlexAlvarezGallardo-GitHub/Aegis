---
type: value-object
service: aegis-wallet-service
layer: domain
tags: [ddd, enum, ledger]
status: implemented
---

# LedgerEntryType

Enum classifying ledger transaction types.

## Values

| Value | Effect | Description |
|-------|--------|-------------|
| `CREDIT` | +balance | Funds added |
| `DEBIT` | -balance | Funds deducted |
| `RESERVE` | -available | Funds locked |
| `RELEASE` | +available | Funds unlocked |

## Used By

- [[02 - Domain Models/LedgerEntry\|LedgerEntry]] value object
