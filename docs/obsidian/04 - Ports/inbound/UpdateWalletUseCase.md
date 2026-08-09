---
type: port
service: aegis-wallet-service
layer: domain
tags: [port, inbound, use-case]
status: implemented
port-type: inbound
---

# UpdateWalletUseCase

Inbound port (interface) for managing an existing wallet — adjusting the balance and changing its status.

```mermaid
sequenceDiagram
    participant Ctrl as WalletController
    participant Port as UpdateWalletUseCase (port)
    participant Svc as UpdateWalletService (impl)
    participant Repo as WalletRepository
    participant Wallet as Wallet (domain)
    participant Event as EventPublisher

    Ctrl->>Port: adjustBalance(command)
    Port->>Svc: delegate
    Svc->>Repo: findById(walletId)
    alt wallet not found or wrong ownership
        Repo-->>Svc: empty → throw WalletNotFoundException
    end
    Svc->>Wallet: adjustBalance(amount, description)
    Wallet->>Wallet: validate ACTIVE, add DEPOSIT/WITHDRAWAL entry
    Svc->>Repo: save(wallet)
    Svc->>Event: publish(WalletBalanceAdjusted)
    Svc-->>Ctrl: WalletDetailResult

    Ctrl->>Port: changeStatus(command)
    Port->>Svc: delegate
    Svc->>Repo: findById(walletId)
    alt wallet not found or wrong ownership
        Repo-->>Svc: empty → throw WalletNotFoundException
    end
    Svc->>Wallet: deactivate(newStatus)
    Wallet->>Wallet: reject ACTIVE target, require balance = 0
    Svc->>Repo: save(wallet)
    Svc-->>Ctrl: WalletDetailResult
```

## Methods

```java
WalletDetailResult adjustBalance(AdjustBalanceCommand command);
WalletDetailResult changeStatus(StatusChangeCommand command);
```

## Commands / Result

```java
record AdjustBalanceCommand(UUID walletId, UUID userId, BigDecimal amount,
                            String description, String correlationId) {}
record StatusChangeCommand(UUID walletId, UUID userId, WalletStatus newStatus) {}
record WalletDetailResult(UUID walletId, UUID userId, BigDecimal balance, String currency,
                          String status, boolean premium, Instant createdAt, Instant updatedAt) {}
```

## Behavior

1. Finds wallet and validates ownership via [[04 - Ports/outbound/WalletRepository|WalletRepository]]
2. `adjustBalance` → validates wallet is ACTIVE, applies the amount, creates a DEPOSIT/WITHDRAWAL entry
3. `adjustBalance` publishes [[03 - Domain Events/WalletBalanceAdjusted|WalletBalanceAdjusted]] via [[04 - Ports/outbound/EventPublisher|EventPublisher]]
4. `changeStatus` → validates zero balance and target ≠ ACTIVE (FROZEN or CLOSED); no event published
5. Persists via [[04 - Ports/outbound/WalletRepository|WalletRepository]]

## Implementation

- **Implemented by**: `UpdateWalletService` in [[01 - Services/Wallet Service|Wallet Service]]
- **Exposed by**: `WalletController` (`PATCH /api/v1/wallets/{id}/balance`, `PATCH /api/v1/wallets/{id}/status`)
