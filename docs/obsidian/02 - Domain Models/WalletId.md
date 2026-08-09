---
type: value-object
service: aegis-wallet-service
layer: domain
tags: [ddd, value-object, uuid]
status: implemented
---

# WalletId

Value object wrapping a UUID v7 identifier.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `value` | UUID | UUID v7 (time-ordered) |

## Factory Methods

- `WalletId.generate()` → new identifier via `UuidV7Generator`
- `WalletId.of(UUID value)` → wraps an existing value

## Generation

- Uses `UuidV7Generator` from [[01 - Services/Common Module|Common Module]]
- Time-ordered for DB index performance

## Used By

- [[02 - Domain Models/Wallet|Wallet]] aggregate root
- [[04 - Ports/outbound/WalletRepository|WalletRepository]] (lookup key)
- JPA entity ID
- REST API path parameter

## JPA Mapping

Stored as `UUID` in `wallets.id` column.
