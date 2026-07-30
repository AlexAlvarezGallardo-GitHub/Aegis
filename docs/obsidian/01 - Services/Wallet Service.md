---
type: service
service: aegis-wallet-service
layer: all
tags: [ddd, hexagonal, java, spring]
status: implemented
port: 8083
database: aegis_wallet
---

# Wallet Service

**Purpose**: Wallet lifecycle management — create, update, deactivate, reactivate wallets with ledger tracking.

## Hexagonal Structure

### Domain (`com.aegis.wallet.domain`)
- **Models**: [[02 - Domain Models/Wallet\|Wallet]], [[02 - Domain Models/WalletId\|WalletId]], [[02 - Domain Models/WalletStatus\|WalletStatus]], [[02 - Domain Models/LedgerEntry\|LedgerEntry]], [[02 - Domain Models/LedgerEntryType\|LedgerEntryType]]
- **Events**: [[03 - Domain Events/WalletCreated\|WalletCreated]], [[03 - Domain Events/WalletUpdated\|WalletUpdated]], [[03 - Domain Events/WalletDeactivated\|WalletDeactivated]], [[03 - Domain Events/WalletReactivated\|WalletReactivated]]
- **Exceptions**: `WalletNotFoundException`, `WalletLimitExceededException`, `InvalidCurrencyException`, `WalletDeactivationException`
- **Inbound Ports**: [[04 - Ports/inbound/CreateWalletUseCase\|CreateWalletUseCase]], [[04 - Ports/inbound/UpdateWalletUseCase\|UpdateWalletUseCase]], [[04 - Ports/inbound/DeactivateWalletUseCase\|DeactivateWalletUseCase]], [[04 - Ports/inbound/ReactivateWalletUseCase\|ReactivateWalletUseCase]]
- **Outbound Ports**: [[04 - Ports/outbound/WalletRepository\|WalletRepository]], [[04 - Ports/outbound/EventPublisher\|EventPublisher]]

### Application (`com.aegis.wallet.application`)
- **Services**: `CreateWalletService`, `UpdateWalletService`, `DeactivateWalletService`, `ReactivateWalletService`
- **DTOs**: `CreateWalletCommand`, `WalletResponse`, `UpdateWalletRequest`
- **Mappers**: `WalletMapper`

### Infrastructure (`com.aegis.wallet.infrastructure`)
- **Persistence**: `WalletJpaEntity`, `WalletJpaRepository`, `WalletRepositoryAdapter`, `LedgerEntryJpaEntity`, `LedgerEntryJpaRepository`, `OutboxEventJpaEntity`, `OutboxEventJpaRepository`, `OutboxRelayScheduler`
- **Messaging**: `KafkaEventPublisher`
- **Config**: `SecurityConfig`, `KafkaConfig`, `SwaggerConfig`

### Web (`com.aegis.wallet.web`)
- **Controllers**: `WalletController`
- **Advice**: `WalletExceptionHandler`

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/wallets` | Create wallet |
| GET | `/api/v1/wallets` | List wallets |
| GET | `/api/v1/wallets/{id}` | Get wallet by ID |
| PATCH | `/api/v1/wallets/{id}` | Update wallet name |
| POST | `/api/v1/wallets/{id}/deactivate` | Deactivate wallet |
| POST | `/api/v1/wallets/{id}/reactivate` | Reactivate wallet |

## Business Rules

1. Max 5 wallets per user (configurable)
2. Deactivation requires balance = €0.00 AND at least 1 other active wallet
3. FROZEN wallets cannot be deactivated directly
4. CLOSED/FROZEN wallets can be reactivated → ACTIVE

## Domain Events Produced

| Event | Topic |
|-------|-------|
| [[03 - Domain Events/WalletCreated\|WalletCreated]] | `aegis.wallet.wallet-created` |
| [[03 - Domain Events/WalletUpdated\|WalletUpdated]] | `aegis.wallet.wallet-updated` |
| [[03 - Domain Events/WalletDeactivated\|WalletDeactivated]] | `aegis.wallet.wallet-deactivated` |
| [[03 - Domain Events/WalletReactivated\|WalletReactivated]] | `aegis.wallet.wallet-reactivated` |

## Dependencies

- **Depends on**: [[01 - Services/Common Module\|Common Module]], PostgreSQL, Kafka
- **Depended by**: [[01 - Services/BFF Service\|BFF Service]] (proxies wallet API)
- **Consumed by**: [[01 - Services/Frontend\|Frontend]] via BFF

## Flyway Migrations

| File | Description |
|------|-------------|
| `V1__create_wallet_tables.sql` | Wallets + ledger + outbox |
