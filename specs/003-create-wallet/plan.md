# Implementation Plan: UC-003 Create Wallet

**Branch**: `feature/003-create-wallet` | **Date**: 2026-07-05

## Implementation Order

```
1. docker-compose.yml          ← Add wallet PostgreSQL database
2. Root pom.xml                ← Add wallet module
3. aegis-wallet-service/       ← New module
   ├── pom.xml
   ├── WalletServiceApplication.java
   ├── domain/model/           (WalletId, Wallet, WalletStatus, LedgerEntry, LedgerEntryType)
   ├── domain/event/           (WalletCreated)
   ├── domain/exception/       (WalletLimitExceeded, InvalidCurrency, etc.)
   ├── domain/port/inbound/    (CreateWalletUseCase)
   ├── domain/port/outbound/   (WalletRepository, EventPublisher)
   ├── application/dto/        (CreateWalletCommand, WalletResponse)
   ├── application/mapper/     (WalletMapper)
   ├── application/service/    (CreateWalletService)
   ├── infrastructure/persistence/ (JPA entities, repositories, adapters)
   ├── infrastructure/messaging/   (KafkaEventPublisher, Outbox)
   ├── infrastructure/config/      (SecurityConfig, KafkaConfig)
   ├── web/controller/         (WalletController)
   ├── web/advice/             (WalletExceptionHandler)
   ├── application.yml
   ├── application-dev.yml
   └── db/migration/V1__create_wallet_tables.sql
4. BFF proxy config            ← Route /api/v1/wallets
5. Frontend                    ← Optional wallet creation form
```

## Key Decisions

| Decision | Choice |
|----------|--------|
| Wallet ID | UUIDv7 (time-ordered, DB-friendly) |
| Balance type | BigDecimal (no floating point errors) |
| Currency | ISO 4217 string (EUR, USD, etc.) |
| Per-user wallet limit | 5 (configurable via `aegis.wallet.max-per-user`) |
| Event publishing | Transactional outbox (same as identity service) |
| Concurrency | Optimistic locking via `@Version` |
| Auth | Stateless JWT (reuse identity service filter pattern) |
