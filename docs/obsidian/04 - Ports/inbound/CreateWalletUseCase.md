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

## Method

```java
WalletResponse createWallet(CreateWalletCommand command);
```

## Behavior

1. Validates per-user wallet limit (max 5) via [[04 - Ports/outbound/WalletRepository\|WalletRepository]]
2. Validates currency (ISO 4217)
3. Creates [[02 - Domain Models/Wallet\|Wallet]] aggregate via factory method
4. Persists via [[04 - Ports/outbound/WalletRepository\|WalletRepository]]
5. Publishes [[03 - Domain Events/WalletCreated\|WalletCreated]] via [[04 - Ports/outbound/EventPublisher\|EventPublisher]]

## Implementation

- **Implemented by**: `CreateWalletService` in [[01 - Services/Wallet Service\|Wallet Service]]
- **Exposed by**: `WalletController` (`POST /api/v1/wallets`)
