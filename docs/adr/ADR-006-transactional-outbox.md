# ADR-006: Transactional Outbox Pattern

## Status

Accepted

## Date

2026-08-07

## Context

A service must update its database (e.g. append a ledger entry and a balance) and
publish a domain event to Kafka atomically. Publishing to Kafka inside the database
transaction is not reliable (the broker call can fail or be delivered after commit),
and committing before publishing loses events on crash.

## Decision

**Use the transactional outbox pattern.** The domain change and an `outbox_events`
row (event type, aggregate id, payload, status) are committed in the same database
transaction. A separate `OutboxRelayScheduler` polls `PENDING` rows and publishes
them to Kafka, then marks them `PUBLISHED`.

- `outbox_events` table in each producing service.
- Relay uses `PESSIMISTIC_WRITE` + `SKIP LOCKED` for multi-instance safety.
- A Micrometer gauge `aegis.outbox.pending_events` exposes the backlog.

## Alternatives Considered

### Alternative 1: Publish to Kafka inside the business transaction
- **Pros**: simple code.
- **Cons**: non-atomic; crash between DB commit and Kafka publish loses the event;
  Kafka failure rolls back the DB change.

### Alternative 2: Event sourcing
- **Pros**: full event history.
- **Cons**: large architectural shift; not needed for the current scope.

### Alternative 3: CDC (Change Data Capture)
- **Pros**: no dual-write; capture from the DB log.
- **Cons**: extra infrastructure (Debezium) and operational complexity.

**Why not chosen**: the outbox pattern is simple, well-understood, and sufficient
for at-least-once publication.

## Consequences

### Positive
- Atomicity between state and event publication.
- At-least-once delivery with retries (event stays PENDING on failure).

### Negative
- Slight latency between commit and Kafka (poll interval).
- Extra table and relay component.

### Risks
- **Risk**: event published twice after relay crash — **Mitigation**: consumer
  deduplication (ADR-008).

## Related Decisions

- ADR-005 (Kafka backbone)
- ADR-008 (idempotency)

## References

- `docs/architecture/outbox-failure.md`
- `OutboxRelayScheduler`
