---
type: port
service: aegis-wallet-service
layer: domain
tags: [port, inbound, use-case]
status: implemented
port-type: inbound
---

# UpdateWalletUseCase

Inbound port (interface) for updating wallet name/alias.

```mermaid
sequenceDiagram
    participant Ctrl as WalletController
    participant Port as UpdateWalletUseCase (port)
    participant Svc as UpdateWalletService (impl)
    participant Repo as WalletRepository
    participant Wallet as Wallet (domain)
    participant Event as EventPublisher

    Ctrl->>Port: updateWallet(walletId, request)
    Port->>Svc: delegate
    Svc->>Repo: findById(walletId)
    alt wallet not found
        Repo-->>Svc: empty → throw WalletNotFoundException
    end
    Svc->>Svc: validate ownership (userId match)
    Svc->>Wallet: updateName(newName)
    Svc->>Repo: save(wallet)
    Svc->>Event: publish(WalletUpdated)
    Svc-->>Ctrl: WalletResponse
```

## Method

```java
WalletResponse updateWallet(UUID walletId, UpdateWalletRequest request);
```

## Behavior

1. Finds wallet via [[04 - Ports/outbound/WalletRepository\|WalletRepository]]
2. Validates ownership (wallet belongs to requesting user)
3. Updates wallet name
4. Persists changes
5. Publishes [[03 - Domain Events/WalletUpdated\|WalletUpdated]]

## Implementation

- **Implemented by**: `UpdateWalletService` in [[01 - Services/Wallet Service\|Wallet Service]]
- **Exposed by**: `WalletController` (`PATCH /api/v1/wallets/{id}`)
