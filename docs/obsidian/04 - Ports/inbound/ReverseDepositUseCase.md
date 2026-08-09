---
type: port
service: aegis-wallet-service
layer: domain
tags: [port, inbound, use-case, ledger, reversal]
status: implemented
port-type: inbound
---

# ReverseDepositUseCase

Inbound port (interface) for reversing a previously applied deposit by appending an immutable REVERSAL ledger entry (ADR-004). The original DEPOSIT entry is never modified.

```mermaid
sequenceDiagram
    participant Ctrl as WalletController
    participant Port as ReverseDepositUseCase (port)
    participant Svc as ReverseDepositService (impl)
    participant Wallet as Wallet (domain)
    participant Repo as WalletRepository

    Ctrl->>Port: reverse(command)
    Port->>Svc: delegate
    Svc->>Repo: findById(walletId)
    alt wallet not found
        Repo-->>Svc: empty → throw WalletNotFoundException
    end
    alt wrong ownership
        Svc->>Svc: userId mismatch → throw WalletNotFoundException
    end
    Svc->>Wallet: reverseDeposit(depositEntryId, reference, "Reversal of deposit")
    alt deposit not found
        Wallet-->>Svc: throw DepositReversalException
    end
    alt entry is not a DEPOSIT
        Wallet-->>Svc: throw DepositReversalException
    end
    alt deposit already reversed
        Wallet-->>Svc: throw DepositReversalException
    end
    Wallet->>Wallet: subtract amount, append REVERSAL entry
    Svc->>Repo: save(wallet)
    Svc-->>Ctrl: ReverseResult(reversalId, walletId, newBalance, reversedAmount, currency, timestamp)
```

## Method

```java
ReverseResult reverse(ReverseCommand command);
```

## Command / Result

```java
record ReverseCommand(UUID walletId, UUID userId, UUID depositEntryId,
                      String reference, String correlationId) {}
record ReverseResult(UUID reversalId, UUID walletId, BigDecimal newBalance,
                     BigDecimal reversedAmount, String currency, Instant timestamp) {}
```

## Behavior

1. Finds the wallet and validates ownership via [[04 - Ports/outbound/WalletRepository|WalletRepository]]
2. Validates the wallet is ACTIVE and the entry is a DEPOSIT that is not already reversed
3. Reverses via `Wallet.reverseDeposit` → appends an immutable [[02 - Domain Models/LedgerEntryType|REVERSAL]] entry referencing the original [[02 - Domain Models/LedgerEntry|LedgerEntry]]
4. Persists via [[04 - Ports/outbound/WalletRepository|WalletRepository]]
5. Returns the reversal receipt; no domain events are published

## Implementation

- **Implemented by**: `ReverseDepositService` in [[01 - Services/Wallet Service|Wallet Service]]
- **Exposed by**: `WalletController` (`POST /api/v1/wallets/{id}/deposits/{depositId}/reversal`)
