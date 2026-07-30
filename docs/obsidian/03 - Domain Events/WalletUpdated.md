---
type: domain-event
service: aegis-wallet-service
layer: domain
tags: [event, kafka, wallet]
status: implemented
topic: aegis.wallet.wallet-updated
---

# WalletUpdated

Published when a wallet's name/alias is updated.

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `walletId` | UUID | Wallet's ID |
| `userId` | UUID | Owner's ID |
| `previousName` | String | Old name |
| `newName` | String | Updated name |
| `timestamp` | Instant | Event time |

## Details

- **Producer**: [[01 - Services/Wallet Service\|Wallet Service]] via [[04 - Ports/outbound/EventPublisher\|EventPublisher]]
- **Topic**: `aegis.wallet.wallet-updated` ([[05 - Infrastructure/Kafka Topics\|Kafka Topics]])
- **Trigger**: `Wallet.updateName()`
