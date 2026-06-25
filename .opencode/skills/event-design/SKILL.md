---
name: event-design
description: Use when designing Kafka events and messaging contracts. Generates event schemas, Kafka topic definitions, producer/consumer implementations, and event versioning.
---

# Event Design

Design Kafka events and messaging contracts following Aegis conventions.

## Input

The user provides:
- Service name producing the event
- Event name and description
- Event payload fields
- Consumer services (if known)

## Event Conventions

### Topic Naming

Format: `aegis.<service>.<event>`

Examples:
- `aegis.payment.completed`
- `aegis.identity.user.registered`
- `aegis.wallet.balance.updated`
- `aegis.fraud.alert.raised`

### Event Schema

Every event MUST include:

```java
public record DomainEvent<T>(
    String eventId,
    String eventType,
    String aggregateId,
    String aggregateType,
    Instant timestamp,
    String version,
    T payload
) {}
```

### Event Versioning

- Version field in event header (e.g., `"1.0"`)
- Backward compatible changes only (add fields, don't remove)
- Breaking changes require new event type
- Schema registry for validation

### Topic Configuration

```yaml
aegis:
  kafka:
    topics:
      <event-name>:
        name: aegis.<service>.<event>
        partitions: 3
        replicas: 3
        retention-ms: 604800000  # 7 days
```

## Generation Output

1. **Event record** (Java record with payload)
2. **Domain event wrapper** (if not exists)
3. **Kafka topic definition** in application.yml
4. **Producer class** with serialization
5. **Consumer class** with deserialization and error handling
6. **Event handler interface** in domain layer
7. **Dead letter topic** configuration
8. **OpenAPI-style event schema** documentation

## Idempotency Rules

- Consumers MUST be idempotent
- Use event ID for deduplication
- Implement outbox pattern for producers
- Handle duplicate delivery gracefully

## Error Handling

- Retry with exponential backoff (3 attempts)
- Dead letter topic for failed messages
- Alerting on DLQ accumulation
- Circuit breaker for downstream failures
