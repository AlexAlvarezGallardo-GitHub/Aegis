---
type: port
service: aegis-wallet-service
layer: domain
tags: [port, inbound, use-case]
status: implemented
port-type: inbound
---

# ReactivateWalletUseCase

Inbound port (interface) for wallet reactivation.

```mermaid
sequenceDiagram
    participant Ctrl as WalletController
    participant Port as ReactivateWalletUseCase (port)
    participant Svc as ReactivateWalletService (impl)
    participant Repo as WalletRepository
    participant Wallet as Wallet (domain)
    participant Event as EventPublisher

    Ctrl->>Port: reactivateWallet(walletId)
    Port->>Svc: delegate
    Svc->>Repo: findById(walletId)
    alt wallet not found
        Repo-->>Svc: empty → throw WalletNotFoundException
    end
    Svc->>Svc: validate ownership (userId match)
    alt wallet is ACTIVE
        Svc-->>Ctrl: throw WalletOperationNotAllowedException (ALREADY_ACTIVE)
    end
    Svc->>Wallet: reactivate()
    Svc->>Repo: save(wallet)
    Svc->>Event: publish(WalletReactivated)
    Svc-->>Ctrl: WalletResponse
```

## Method

```java
WalletResponse reactivateWallet(UUID walletId);
```

## Behavior

- CLOSED wallets → ACTIVE
- FROZEN wallets → ACTIVE
- Already ACTIVE wallets → throws exception

## Implementation

- **Implemented by**: `ReactivateWalletService` in [[01 - Services/Wallet Service\|Wallet Service]]
- **Exposed by**: `WalletController` (`POST /api/v1/wallets/{id}/reactivate`)
