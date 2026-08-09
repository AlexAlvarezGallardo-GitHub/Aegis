---
type: port
service: aegis-identity-service, aegis-wallet-service, aegis-fraud-service
layer: domain
tags: [port, outbound, messaging, kafka]
status: implemented
port-type: outbound
---

# EventPublisher

Outbound port for publishing domain events to Kafka via transactional outbox. Each service defines its own interface with overloads for its domain event types.

## Method (per event type)

```java
void publish(<DomainEvent> event);
```

## Service Variants

### Identity (`com.aegis.identity.domain.port.outbound.EventPublisher`)

- [[03 - Domain Events/UserRegistered|UserRegistered]] → `aegis.identity.user-registered`
- [[03 - Domain Events/UserAuthenticated|UserAuthenticated]] → `aegis.identity.user-authenticated`
- [[03 - Domain Events/UserAccountLocked|UserAccountLocked]] → `aegis.identity.user-account-locked`

### Wallet (`com.aegis.wallet.domain.port.outbound.EventPublisher`)

- [[03 - Domain Events/WalletCreated|WalletCreated]] → `aegis.wallet.created`
- [[03 - Domain Events/WalletBalanceAdjusted|WalletBalanceAdjusted]] → `aegis.wallet.balance.adjusted`
- [[03 - Domain Events/FundsDeposited|FundsDeposited]] → `wallet.funds.deposited`

### Fraud (`com.aegis.fraud.domain.port.outbound.EventPublisher`)

- [[03 - Domain Events/FraudAssessmentCompleted|FraudAssessmentCompleted]] → `fraud.assessment.completed`

## Implementation

- **Adapter**: `KafkaEventPublisher` in each service's `infrastructure/messaging/`
- **Pattern**: Transactional outbox (same-DB write + scheduler relay)
- **Outbox Table**: `outbox_events` (service-specific)
- **Scheduler**: `OutboxRelayScheduler` polls every 1s
- **Topics**: configured per service in `KafkaTopicsProperties` / `application.yml`
