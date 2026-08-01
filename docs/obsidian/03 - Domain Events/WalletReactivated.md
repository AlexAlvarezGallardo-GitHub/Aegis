---
type: domain-event
service: aegis-wallet-service
layer: domain
tags: [event, kafka, wallet]
status: planned
topic: aegis.wallet.wallet-reactivated
---

> **Status: Planned** — This event is documented but not yet implemented in code. The wallet reactivation flow exists but does not yet publish a domain event.

# WalletReactivated

Published when a CLOSED or FROZEN wallet is reactivated.

```mermaid
graph LR
    Wallet[Wallet Service] -->|publishes| Topic[aegis.wallet.wallet-reactivated]
    Topic --> Audit[Audit Service]
    style Wallet fill:#bbf,stroke:#333,color:#000
    style Topic fill:#fdb,stroke:#333,color:#000
    style Audit fill:#bfb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant Wallet as Wallet (Domain)
    participant Svc as ReactivateWalletService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic
    participant Audit as Audit Consumer

    Wallet->>Svc: reactivate()
    Svc->>Svc: validate status != ACTIVE
    Svc->>Pub: publish(WalletReactivated)
    Pub->>DB: INSERT outbox_event (payload=WalletReactivated JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
    Kafka->>Audit: Consume (group=audit-group)
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `walletId` | UUID | Wallet's ID |
| `userId` | UUID | Owner's ID |
| `previousStatus` | String | Status before reactivation (CLOSED/FROZEN) |
| `newStatus` | String | Always ACTIVE |
| `timestamp` | Instant | Event time |

## Details

- **Producer**: [[01 - Services/Wallet Service\|Wallet Service]] via [[04 - Ports/outbound/EventPublisher\|EventPublisher]]
- **Topic**: `aegis.wallet.wallet-reactivated` ([[05 - Infrastructure/Kafka Topics\|Kafka Topics]])
- **Trigger**: `Wallet.reactivate()`
