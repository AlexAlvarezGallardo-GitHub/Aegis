---
type: domain-event
service: aegis-wallet-service
layer: domain
tags: [event, kafka, wallet]
status: implemented
topic: aegis.wallet.wallet-created
---

# WalletCreated

Published when a new wallet is created.

```mermaid
graph LR
    Wallet[Wallet Service] -->|publishes| Topic[aegis.wallet.wallet-created]
    Topic --> Report[Reporting Service]
    Topic --> Audit[Audit Service]
    style Wallet fill:#bbf,stroke:#333,color:#000
    style Topic fill:#fdb,stroke:#333,color:#000
    style Report fill:#bfb,stroke:#333,color:#000
    style Audit fill:#bfb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant Wallet as Wallet (Domain)
    participant Svc as CreateWalletService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic
    participant Report as Reporting Consumer
    participant Audit as Audit Consumer

    Wallet->>Svc: create()
    Svc->>Pub: publish(WalletCreated)
    Pub->>DB: INSERT outbox_event (payload=WalletCreated JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
    Kafka->>Report: Consume (group=reporting-group)
    Kafka->>Audit: Consume (group=audit-group)
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `walletId` | UUID | New wallet's ID |
| `userId` | UUID | Owner's ID |
| `currency` | String | ISO 4217 currency |
| `balance` | BigDecimal | Initial balance (0) |
| `timestamp` | Instant | Event time |

## Details

- **Producer**: [[01 - Services/Wallet Service\|Wallet Service]] via [[04 - Ports/outbound/EventPublisher\|EventPublisher]]
- **Topic**: `aegis.wallet.wallet-created` ([[05 - Infrastructure/Kafka Topics\|Kafka Topics]])
- **Schema**: `specs/003-create-wallet/contracts/events/wallet-created-event.json`
- **Trigger**: `Wallet.create()` factory method
