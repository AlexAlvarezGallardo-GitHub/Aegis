---
type: port
service: aegis-wallet-service
layer: domain
tags: [port, inbound, use-case, deposit]
status: implemented
port-type: inbound
---

# DepositFundsUseCase

Inbound port (interface) for depositing funds into a wallet.

```mermaid
sequenceDiagram
    participant Ctrl as WalletController
    participant Port as DepositFundsUseCase (port)
    participant Svc as DepositFundsService (impl)
    participant Wallet as Wallet (domain)
    participant Repo as WalletRepository
    participant Event as EventPublisher

    Ctrl->>Port: deposit(command)
    Port->>Svc: delegate
    Svc->>Repo: findById(walletId)
    alt wallet not found
        Repo-->>Svc: empty → throw WalletNotFoundException
    end
    alt wrong ownership
        Svc->>Svc: userId mismatch → throw WalletNotFoundException
    end
    Svc->>Wallet: getLedgerEntries()
    alt duplicate reference
        Svc->>Svc: throw DuplicateDepositException
    end
    Svc->>Wallet: depositFunds(amount, source, ref)
    Wallet->>Wallet: adjust balance, add LedgerEntry
    Svc->>Repo: save(wallet)
    Svc->>Event: publish(FundsDeposited event)
    Svc-->>Ctrl: DepositResult
```

## Method

```java
DepositResult deposit(DepositCommand command);
```

## Behavior

1. Validates wallet exists and belongs to the user
2. Checks idempotency (rejects duplicate external references)
3. Validates wallet is ACTIVE and amount is positive
4. Updates wallet balance and creates DEPOSIT ledger entry
5. Persists via [[04 - Ports/outbound/WalletRepository\|WalletRepository]]
6. Publishes [[03 - Domain Events/FundsDeposited\|FundsDeposited]] via [[04 - Ports/outbound/EventPublisher\|EventPublisher]]

## Implementation

- **Implemented by**: `DepositFundsService` in [[01 - Services/Wallet Service\|Wallet Service]]
- **Exposed by**: `WalletController` (`POST /api/v1/wallets/{id}/deposits`)
