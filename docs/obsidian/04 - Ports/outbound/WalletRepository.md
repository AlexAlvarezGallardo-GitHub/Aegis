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
| `save(wallet)` → `Wallet` | Persist new or updated wallet |
| `findById(WalletId walletId)` → `Optional<Wallet>` | Lookup by [[02 - Domain Models/WalletId\|WalletId]] |
| `findByUserId(userId)` → `List<Wallet>` | List user's wallets |
| `countByUserId(userId)` → `long` | Count user's wallets (limit check) |

## Implementation

- **Adapter**: `WalletRepositoryAdapter` in `infrastructure/persistence/`
- **Spring Data**: `WalletJpaRepository`, `LedgerEntryJpaRepository`
- **JPA Entities**: `WalletJpaEntity`, `LedgerEntryJpaEntity`

## Used By

- [[04 - Ports/inbound/CreateWalletUseCase|CreateWalletUseCase]]
- [[04 - Ports/inbound/UpdateWalletUseCase|UpdateWalletUseCase]]
- [[04 - Ports/inbound/DepositFundsUseCase|DepositFundsUseCase]]
- [[04 - Ports/inbound/ReverseDepositUseCase|ReverseDepositUseCase]]
- [[04 - Ports/inbound/ListWalletsUseCase|ListWalletsUseCase]]
- [[04 - Ports/inbound/GetWalletDetailUseCase|GetWalletDetailUseCase]]
