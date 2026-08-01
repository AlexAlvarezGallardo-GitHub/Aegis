---
type: domain-event
service: aegis-identity-service
layer: domain
tags: [event, kafka, auth]
status: implemented
topic: aegis.identity.user-authenticated
---

# UserAuthenticated

Published on successful user login.

```mermaid
graph LR
    Identity[Identity Service] -->|publishes| Topic[aegis.identity.user-authenticated]
    Topic --> Audit[Audit Service]
    style Identity fill:#bbf,stroke:#333,color:#000
    style Topic fill:#fdb,stroke:#333,color:#000
    style Audit fill:#bfb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant User as User (Domain)
    participant Svc as AuthenticateUserService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic
    participant Audit as Audit Consumer

    User->>Svc: authenticate()
    Svc->>Pub: publish(UserAuthenticated)
    Pub->>DB: INSERT outbox_event (payload=UserAuthenticated JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
    Kafka->>Audit: Consume (group=audit-group)
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `userId` | UUID | Authenticated user's ID |
| `email` | String | User's email |
| `timestamp` | Instant | Event time |
| `ipAddress` | String | Request origin IP |

## Details

- **Producer**: [[01 - Services/Identity Service\|Identity Service]] via [[04 - Ports/outbound/EventPublisher\|EventPublisher]]
- **Topic**: `aegis.identity.user-authenticated` ([[05 - Infrastructure/Kafka Topics\|Kafka Topics]])
- **Schema**: `specs/002-user-authentication/contracts/user-authenticated-event.json`
- **Trigger**: `User.authenticate()` success

## Consumers

- Audit service (future)
