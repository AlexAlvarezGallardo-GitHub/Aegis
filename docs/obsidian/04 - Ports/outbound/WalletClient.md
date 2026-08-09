---
type: port
service: aegis-bff-service
layer: domain
tags: [port, outbound, client, bff, wallet]
status: implemented
port-type: outbound
---

# WalletClient

Outbound port for communicating with the Wallet Service from the BFF.

## Methods

| Method | Description |
|--------|-------------|
| `createWallet(accessToken, userId, currency, correlationId)` → `JsonNode` | Create a wallet |
| `listWallets(accessToken, userId)` → `JsonNode` | List a user's wallets |
| `getWallet(accessToken, userId, walletId)` → `JsonNode` | Get a single wallet |
| `adjustBalance(accessToken, userId, walletId, type, amount, reason, correlationId)` → `JsonNode` | Adjust wallet balance |
| `depositFunds(accessToken, userId, walletId, amount, method, reference, correlationId)` → `JsonNode` | Deposit funds |
| `updateStatus(accessToken, userId, walletId, status)` → `JsonNode` | Update wallet status |

## Implementation

- **Adapter**: `RestWalletClient` in `infrastructure/client/` (REST via `RestClient`)
- **Consumed by**: `BffWalletController` in [[01 - Services/BFF Service|BFF Service]]
