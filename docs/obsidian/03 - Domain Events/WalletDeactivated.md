---
type: domain-event
service: aegis-wallet-service
layer: domain
tags: [event, kafka, wallet]
status: planned
topic: aegis.wallet.wallet-deactivated
---

> **Status: Planned** — This event is documented but not yet implemented in code. The wallet deactivation flow exists but does not yet publish a domain event.

# WalletDeactivated

Published when a wallet is successfully deactivated (closed).

```mermaid
graph LR
    Wallet[Wallet Service] -->|publishes| Topic[aegis.wallet.wallet-deactivated]
    Topic --> Report[Reporting Service]
    Topic --> Audit[Audit Service]
    style Wallet fill:#bbf,stroke:#333
    style Topic fill:#fdb,stroke:#333
    style Report fill:#bfb,stroke:#333
    style Audit fill:#bfb,stroke:#333
```

```mermaid
sequenceDiagram
    participant Wallet as Wallet (Domain)
    participant Svc as DeactivateWalletService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic
    participant Report as Reporting Consumer

    Wallet->>Svc: deactivate()
    Svc->>Svc: validate balance == 0
    Svc->>Pub: publish(WalletDeactivated)
    Pub->>DB: INSERT outbox_event (payload=WalletDeactivated JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
    Kafka->>Report: Consume (group=reporting-group)
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `walletId` | UUID | Wallet's ID |
| `userId` | UUID | Owner's ID |
| `previousStatus` | String | Status before deactivation |
| `timestamp` | Instant | Event time |

## Details

- **Producer**: [[01 - Services/Wallet Service\|Wallet Service]] via [[04 - Ports/outbound/EventPublisher\|EventPublisher]]
- **Topic**: `aegis.wallet.wallet-deactivated` ([[05 - Infrastructure/Kafka Topics\|Kafka Topics]])
- **Trigger**: `Wallet.deactivate()` (after business rule validation)
