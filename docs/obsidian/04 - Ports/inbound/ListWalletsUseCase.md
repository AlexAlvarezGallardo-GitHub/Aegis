---
type: port
service: aegis-wallet-service
layer: domain
tags: [port, inbound, use-case, query]
status: implemented
port-type: inbound
---

# ListWalletsUseCase

Inbound port (interface) for listing the wallets belonging to a user.

```mermaid
sequenceDiagram
    participant Ctrl as WalletController
    participant Port as ListWalletsUseCase (port)
    participant Svc as WalletQueryService (impl)
    participant Repo as WalletRepository

    Ctrl->>Port: listByUser(userId)
    Port->>Svc: delegate
    Svc->>Repo: findByUserId(userId)
    Repo-->>Svc: List<Wallet>
    Svc->>Svc: map to List<Result> (balance, currency, status, premium)
    Svc-->>Ctrl: List<Result>
```

## Method

```java
List<Result> listByUser(UUID userId);
```

## Result

```java
record Result(UUID walletId, UUID userId, BigDecimal balance, String currency,
              String status, boolean premium, Instant createdAt) {}
```

## Behavior

1. Queries wallets via [[04 - Ports/outbound/WalletRepository|WalletRepository]] (`findByUserId`)
2. Maps each [[02 - Domain Models/Wallet|Wallet]] to a summary `Result` including the [[02 - Domain Models/Wallet|isPremium()]] flag
3. Returns an empty list when the user has no wallets; no domain events are published

## Implementation

- **Implemented by**: `WalletQueryService` in [[01 - Services/Wallet Service|Wallet Service]]
- **Exposed by**: `WalletController` (`GET /api/v1/wallets`)
