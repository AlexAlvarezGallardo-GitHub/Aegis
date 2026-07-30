---
type: port
service: aegis-wallet-service
layer: domain
tags: [port, outbound, repository]
status: implemented
port-type: outbound
---

# WalletRepository

Outbound port for wallet persistence.

## Methods

| Method | Description |
|--------|-------------|
| `findById(walletId)` | Lookup by ID |
| `findByUserId(userId)` | List user's wallets |
| `save(wallet)` | Persist new or updated wallet |
| `countByUserId(userId)` | Count user's wallets |
| `countActiveWalletsByUserId(userId)` | Count active wallets (deactivation check) |

## Implementation

- **Adapter**: `WalletRepositoryAdapter` in `infrastructure/persistence/`
- **Spring Data**: `WalletJpaRepository`, `LedgerEntryJpaRepository`
- **JPA Entities**: `WalletJpaEntity`, `LedgerEntryJpaEntity`
