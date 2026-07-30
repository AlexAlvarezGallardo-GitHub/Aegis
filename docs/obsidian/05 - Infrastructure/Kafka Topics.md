---
type: infrastructure
tags: [kafka, messaging, events]
status: implemented
---

# Kafka Topics

All domain event topics in the Aegis platform.

## Identity Service Topics

| Topic | Event | Partitions | Retention |
|-------|-------|-----------|-----------|
| `aegis.identity.user-registered` | [[03 - Domain Events/UserRegistered\|UserRegistered]] | 3 | 7 days |
| `aegis.identity.user-authenticated` | [[03 - Domain Events/UserAuthenticated\|UserAuthenticated]] | 3 | 7 days |
| `aegis.identity.user-account-locked` | [[03 - Domain Events/UserAccountLocked\|UserAccountLocked]] | 1 | 30 days |

## Wallet Service Topics

| Topic | Event | Partitions | Retention |
|-------|-------|-----------|-----------|
| `aegis.wallet.wallet-created` | [[03 - Domain Events/WalletCreated\|WalletCreated]] | 3 | 7 days |
| `aegis.wallet.wallet-updated` | [[03 - Domain Events/WalletUpdated\|WalletUpdated]] | 3 | 7 days |
| `aegis.wallet.wallet-deactivated` | [[03 - Domain Events/WalletDeactivated\|WalletDeactivated]] | 3 | 7 days |
| `aegis.wallet.wallet-reactivated` | [[03 - Domain Events/WalletReactivated\|WalletReactivated]] | 3 | 7 days |

## Naming Convention

```
aegis.<service>.<event-name>
```

## Delivery Semantics

- **Pattern**: Transactional outbox
- **Guarantee**: At-least-once delivery
- **Serialization**: JSON (avro planned for future)
