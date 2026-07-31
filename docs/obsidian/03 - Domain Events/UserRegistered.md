---
type: domain-event
service: aegis-identity-service
layer: domain
tags: [event, kafka, identity]
status: implemented
topic: aegis.identity.user-registered
---

# UserRegistered

Published when a new user successfully registers.

```mermaid
graph LR
    Identity[Identity Service] -->|publishes| Topic[aegis.identity.user-registered]
    Topic --> Audit[Audit Service]
    Topic --> Notification[Notification Service]
    style Identity fill:#bbf,stroke:#333
    style Topic fill:#fdb,stroke:#333
    style Audit fill:#bfb,stroke:#333
    style Notification fill:#bfb,stroke:#333
```

```mermaid
sequenceDiagram
    participant User as User (Domain)
    participant Svc as RegisterUserService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic
    participant Audit as Audit Consumer

    User->>Svc: register()
    Svc->>Pub: publish(UserRegistered)
    Pub->>DB: INSERT outbox_event (payload=UserRegistered JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
    Kafka->>Audit: Consume (group=audit-group)
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `userId` | UUID | New user's ID |
| `email` | String | User's email |
| `status` | String | Initial status |
| `timestamp` | Instant | Event time |

## Details

- **Producer**: [[01 - Services/Identity Service\|Identity Service]] via [[04 - Ports/outbound/EventPublisher\|EventPublisher]]
- **Topic**: `aegis.identity.user-registered` ([[05 - Infrastructure/Kafka Topics\|Kafka Topics]])
- **Schema**: `specs/001-user-registration/contracts/user-registered-event.json`
- **Trigger**: `User.register()` in [[02 - Domain Models/User\|User]] aggregate

## Consumers

- Audit service (future)
- Notification service (future)
