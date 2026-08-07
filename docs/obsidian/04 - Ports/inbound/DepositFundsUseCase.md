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
    Svc->>Wallet: depositFunds(amount, source, reference, "Deposit")
    Wallet->>Wallet: validate ACTIVE + positive amount, add DEPOSIT entry
    Svc->>Repo: save(wallet)
    Svc->>Event: publish(FundsDeposited)
    Svc-->>Ctrl: DepositResult(depositId, walletId, newBalance, amount, currency, source, reference, timestamp)
```

## Method

```java
DepositResult deposit(DepositCommand command);
```

## Command / Result

```java
record DepositCommand(UUID walletId, UUID userId, BigDecimal amount, String currency,
                      String source, String reference, String correlationId) {}
record DepositResult(UUID depositId, UUID walletId, BigDecimal newBalance, BigDecimal amount,
                     String currency, String source, String reference, Instant timestamp) {}
```

## Behavior

1. Validates wallet exists and belongs to the user ([[04 - Ports/outbound/WalletRepository|WalletRepository]])
2. Checks idempotency (rejects duplicate external references; DB unique partial index as race-condition guard)
3. Validates wallet is ACTIVE and amount is positive
4. Updates wallet balance and creates a DEPOSIT ledger entry
5. Persists via [[04 - Ports/outbound/WalletRepository|WalletRepository]]
6. Publishes [[03 - Domain Events/FundsDeposited|FundsDeposited]] via [[04 - Ports/outbound/EventPublisher|EventPublisher]]

## Implementation

- **Implemented by**: `DepositFundsService` in [[01 - Services/Wallet Service|Wallet Service]]
- **Exposed by**: `WalletController` (`POST /api/v1/wallets/{id}/deposits`)
