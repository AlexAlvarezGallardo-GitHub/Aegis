---
type: port
service: aegis-wallet-service
layer: domain
tags: [port, inbound, use-case]
status: implemented
port-type: inbound
---

# DeactivateWalletUseCase

Inbound port (interface) for wallet deactivation.

```mermaid
sequenceDiagram
    participant Ctrl as WalletController
    participant Port as DeactivateWalletUseCase (port)
    participant Svc as DeactivateWalletService (impl)
    participant Repo as WalletRepository
    participant Wallet as Wallet (domain)
    participant Event as EventPublisher

    Ctrl->>Port: deactivateWallet(walletId)
    Port->>Svc: delegate
    Svc->>Repo: findById(walletId)
    alt wallet not found
        Repo-->>Svc: empty → throw WalletNotFoundException
    end
    Svc->>Svc: validate ownership (userId match)
    alt balance != 0
        Svc-->>Ctrl: throw WalletOperationNotAllowedException (NON_ZERO_BALANCE)
    end
    alt wallet is FROZEN
        Svc-->>Ctrl: throw WalletOperationNotAllowedException (FROZEN_WALLET)
    end
    alt wallet already CLOSED
        Svc-->>Ctrl: throw WalletOperationNotAllowedException (ALREADY_CLOSED)
    end
    Svc->>Repo: countActiveWalletsByUserId(userId)
    alt only one active wallet
        Svc-->>Ctrl: throw WalletDeactivationException (LAST_ACTIVE_WALLET)
    end
    Svc->>Wallet: deactivate(targetStatus)
    Svc->>Repo: save(wallet)
    Svc->>Event: publish(WalletDeactivated)
    Svc-->>Ctrl: WalletResponse
```

## Method

```java
WalletResponse deactivateWallet(UUID walletId);
```

## Business Rules (CRITICAL)

1. Balance must be exactly €0.00
2. User must have at least ONE other active wallet
3. FROZEN wallets cannot be deactivated directly (must reactivate first)
4. Already CLOSED wallets cannot be deactivated again

Violations throw `WalletDeactivationException` with a specific reason code.

## Implementation

- **Implemented by**: `DeactivateWalletService` in [[01 - Services/Wallet Service\|Wallet Service]]
- **Exposed by**: `WalletController` (`POST /api/v1/wallets/{id}/deactivate`)
- **Validates via**: [[04 - Ports/outbound/WalletRepository\|WalletRepository]] (countActiveWalletsByUserId)
