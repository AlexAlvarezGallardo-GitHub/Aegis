---
type: domain-event
service: aegis-identity-service
layer: domain
tags: [event, kafka, auth]
status: implemented
topic: aegis.identity.user-authenticated
---

# UserAuthenticated

Published on every login attempt — both success and failure — with a `success` flag.

```mermaid
graph LR
    Identity[Identity Service] -->|publishes| Topic[aegis.identity.user-authenticated]
    style Identity fill:#bbf,stroke:#333,color:#000
    style Topic fill:#fdb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant User as User (Domain)
    participant Svc as AuthenticateUserService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic

    User->>Svc: authenticate()
    alt invalid credentials
        User-->>Svc: UserAuthenticated(success=false, INVALID_CREDENTIALS)
    else success
        User-->>Svc: UserAuthenticated(success=true)
    end
    Svc->>Pub: publish(UserAuthenticated)
    Pub->>DB: INSERT outbox_event (payload=UserAuthenticated JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `eventType` | String | `USER_AUTHENTICATED` |
| `schemaVersion` | String | `1.0` |
| `userId` | UUID | Authenticating user's ID |
| `email` | String | User's email |
| `timestamp` | Instant | Event time |
| `success` | boolean | Whether authentication succeeded |
| `failureReason` | String | Failure reason (e.g., `INVALID_CREDENTIALS`) or `null` |
| `correlationId` | String | Correlation ID for tracing |

## Details

- **Producer**: [[01 - Services/Identity Service|Identity Service]] via [[04 - Ports/outbound/EventPublisher|EventPublisher]]
- **Topic**: `aegis.identity.user-authenticated` ([[05 - Infrastructure/Kafka Topics|Kafka Topics]])
- **Trigger**: `User.authenticate()` in [[02 - Domain Models/User|User]] aggregate (emitted on success and failure)

## Consumers

- Ninguno actualmente (sin consumidor configurado)
