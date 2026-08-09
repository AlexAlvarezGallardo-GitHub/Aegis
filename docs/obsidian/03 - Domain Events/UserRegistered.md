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
    style Identity fill:#bbf,stroke:#333,color:#000
    style Topic fill:#fdb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant User as User (Domain)
    participant Svc as RegisterUserService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic

    User->>Svc: register()
    Svc->>Pub: publish(UserRegistered)
    Pub->>DB: INSERT outbox_event (payload=UserRegistered JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `eventType` | String | `USER_REGISTERED` |
| `schemaVersion` | String | `1.0` |
| `userId` | UUID | New user's ID |
| `email` | String | User's email |
| `firstName` | String | User's first name |
| `lastName` | String | User's last name |
| `registeredAt` | Instant | Registration time |
| `correlationId` | String | Correlation ID for tracing |

## Details

- **Producer**: [[01 - Services/Identity Service|Identity Service]] via [[04 - Ports/outbound/EventPublisher|EventPublisher]]
- **Topic**: `aegis.identity.user-registered` ([[05 - Infrastructure/Kafka Topics|Kafka Topics]])
- **Trigger**: `User.register()` in [[02 - Domain Models/User|User]] aggregate

## Consumers

- Ninguno actualmente (sin consumidor configurado)
