# ADR-005: Kafka as the Event Backbone

## Status

Accepted

## Date

2026-08-07

## Context

The platform is a set of Spring Boot microservices that must react to domain
events (user registered, funds deposited, fraud assessed) without tight coupling.
Options for inter-service communication: synchronous REST calls between services,
a shared database, a message broker, or an event bus.

Constraints:
- Services must be independently deployable (database-per-service, see ADR-002).
- A deposit must fan out to Audit, Reporting and Fraud without the Wallet service
  knowing about them.
- Delivery must be at-least-once with the possibility of duplicates (requires
  idempotent consumers).

## Decision

**Use Apache Kafka as the event backbone.** Services communicate through domain
events published to Kafka topics; consumers subscribe to the topics they care
about. The transactional outbox (ADR-006) guarantees reliable publication.

- Topics are named by domain flow (`wallet.funds.deposited`, `fraud.assessment.completed`).
- Events carry a standard envelope (eventId, eventType, schemaVersion, occurredAt,
  correlationId — see ADR-009).
- Consumers deduplicate by `eventId` (processed_events) for idempotency (ADR-008).

## Alternatives Considered

### Alternative 1: Synchronous REST between services
- **Pros**: simple, familiar.
- **Cons**: couples services; the Wallet service would need to call Audit, Reporting
  and Fraud synchronously and handle partial failures; no replay.

### Alternative 2: Shared database
- **Pros**: trivial reads.
- **Cons**: violates database-per-service; couples schemas; no event history.

### Alternative 3: RabbitMQ (queue broker)
- **Pros**: good at work queues.
- **Cons**: weaker ordering/partitioning guarantees; less suited to long-lived event
  streams and replay.

**Why not chosen**: Kafka provides durable partitioned topics, per-partition
ordering, replay, and a natural fit for event-driven fan-out and the outbox pattern.

## Consequences

### Positive
- Loose coupling; services evolve independently.
- Durable event log enabling replay and audit.
- Per-partition ordering for per-aggregate flows.

### Negative
- Kafka is an operational dependency (ZooKeeper/KRaft, partitions, consumer groups).
- At-least-once delivery means consumers must be idempotent.

### Risks
- **Risk**: ordering across partitions not guaranteed — **Mitigation**: partition
  key = aggregate id (see ADR on partitioning).
- **Risk**: consumer lag — **Mitigation**: SLI/SLO + runbooks.

## Related Decisions

- ADR-006 (transactional outbox)
- ADR-008 (idempotency)
- ADR-009 (event versioning)
- ADR-010 (retry/DLT)

## References

- `docs/architecture/kafka-partitioning.md`
- `docs/obsidian/05 - Infrastructure/Kafka Topics.md`
