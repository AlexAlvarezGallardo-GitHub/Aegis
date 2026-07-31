---
type: domain-event
service: aegis-wallet-service
layer: domain
tags: [event, kafka, wallet]
status: planned
topic: aegis.wallet.wallet-updated
---

> **Status: Planned** — This event is documented but not yet implemented in code. The wallet update flow exists but does not yet publish a domain event.

# WalletUpdated

Published when a wallet's name/alias is updated.

```mermaid
graph LR
    Wallet[Wallet Service] -->|publishes| Topic[aegis.wallet.wallet-updated]
    Topic --> Audit[Audit Service]
    style Wallet fill:#bbf,stroke:#333
    style Topic fill:#fdb,stroke:#333
    style Audit fill:#bfb,stroke:#333
```

```mermaid
sequenceDiagram
    participant Wallet as Wallet (Domain)
    participant Svc as UpdateWalletService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic
    participant Audit as Audit Consumer

    Wallet->>Svc: updateName()
    Svc->>Pub: publish(WalletUpdated)
    Pub->>DB: INSERT outbox_event (payload=WalletUpdated JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
    Kafka->>Audit: Consume (group=audit-group)
```

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
