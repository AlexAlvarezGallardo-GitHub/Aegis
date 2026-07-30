---
type: port
service: aegis-identity-service
layer: domain
tags: [port, outbound, messaging, kafka]
status: implemented
port-type: outbound
---

# EventPublisher

Outbound port for publishing domain events to Kafka via transactional outbox.

## Method

```java
void publish(DomainEvent event);
```

## Implementation

- **Adapter (Identity)**: `KafkaEventPublisher` in `infrastructure/messaging/`
- **Adapter (Wallet)**: `KafkaEventPublisher` in `infrastructure/messaging/`
- **Pattern**: Transactional outbox (same-DB write + scheduler relay)
- **Outbox Table**: `outbox_events` (service-specific)
- **Scheduler**: `OutboxRelayScheduler` polls every 1s

## Events Published

**Identity:**
- [[03 - Domain Events/UserRegistered\|UserRegistered]] → `aegis.identity.user-registered`
- [[03 - Domain Events/UserAuthenticated\|UserAuthenticated]] → `aegis.identity.user-authenticated`
- [[03 - Domain Events/UserAccountLocked\|UserAccountLocked]] → `aegis.identity.user-account-locked`

**Wallet:**
- [[03 - Domain Events/WalletCreated\|WalletCreated]] → `aegis.wallet.wallet-created`
- [[03 - Domain Events/WalletUpdated\|WalletUpdated]] → `aegis.wallet.wallet-updated`
- [[03 - Domain Events/WalletDeactivated\|WalletDeactivated]] → `aegis.wallet.wallet-deactivated`
- [[03 - Domain Events/WalletReactivated\|WalletReactivated]] → `aegis.wallet.wallet-reactivated`
