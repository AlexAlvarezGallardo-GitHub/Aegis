---
type: port
service: aegis-wallet-service
layer: domain
tags: [port, inbound, use-case]
status: implemented
port-type: inbound
---

# CreateWalletUseCase

Inbound port (interface) for wallet creation.

```mermaid
sequenceDiagram
    participant Ctrl as WalletController
    participant Port as CreateWalletUseCase (port)
    participant Svc as CreateWalletService (impl)
    participant Repo as WalletRepository
    participant Wallet as Wallet (domain)
    participant Event as EventPublisher

    Ctrl->>Port: execute(command)
    Port->>Svc: delegate
    Svc->>Repo: countByUserId(userId)
    alt wallet limit exceeded (>= 5)
        Svc-->>Ctrl: throw WalletLimitExceededException
    end
    Svc->>Wallet: Wallet.create(userId, currency)
    Wallet->>Wallet: validate ISO 4217 currency
    Wallet->>Wallet: add OPENING ledger entry
    Svc->>Repo: save(wallet)
    Svc->>Event: publish(WalletCreated)
    Svc-->>Ctrl: Result(walletId, userId, balance, currency, status, createdAt)
```

## Method

```java
Result execute(Command command);
```

## Command / Result

```java
record Command(UUID userId, String currency, String correlationId) {}
record Result(UUID walletId, UUID userId, BigDecimal balance, String currency,
              String status, Instant createdAt) {}
```

## Behavior

1. Validates per-user wallet limit (max 5) via [[04 - Ports/outbound/WalletRepository|WalletRepository]]
2. Creates [[02 - Domain Models/Wallet|Wallet]] aggregate via factory method (validates ISO 4217 currency)
3. Persists via [[04 - Ports/outbound/WalletRepository|WalletRepository]]
4. Publishes [[03 - Domain Events/WalletCreated|WalletCreated]] via [[04 - Ports/outbound/EventPublisher|EventPublisher]]

## Implementation

- **Implemented by**: `CreateWalletService` in [[01 - Services/Wallet Service|Wallet Service]]
- **Exposed by**: `WalletController` (`POST /api/v1/wallets`)
