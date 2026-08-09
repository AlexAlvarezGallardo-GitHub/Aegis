# ADR-010: Retry Policy and Dead Letter Topics

## Status

Accepted

## Date

2026-08-07

## Context

Kafka consumers fail transiently (DB blips, downstream timeouts) and permanently
(poison messages, schema drift). A naive consumer that retries forever blocks its
partition; one that drops the record loses data.

## Decision

**Use a `DefaultErrorHandler` with a fixed back-off plus a `DeadLetterPublishingRecoverer`.**

- Transient failures are retried up to `aegis.kafka.retry.max-attempts` (default 3)
  with `backoff-ms` (default 1000).
- After the attempts are exhausted, the failed record is published to
  `<topic>.dlt`, preserving key, value and headers.
- All consumer services (Audit, Reporting, Fraud) apply this configuration.

## Alternatives Considered

### Alternative 1: Retry forever (infinite back-off)
- **Pros**: no message loss.
- **Cons**: blocks the partition indefinitely; no visibility into poison messages.

### Alternative 2: Drop on failure
- **Pros**: simple.
- **Cons**: silent data loss.

### Alternative 3: Per-topic retry topics (`*.retry`) with repartitioning
- **Pros**: isolates slow consumers from the main partition.
- **Cons**: more topics and configuration; not needed at the current scale.

**Why not chosen**: fixed back-off + DLT is the standard, simple, observable
default. Retry topics remain a scalability option.

## Consequences

### Positive
- Poison messages are visible in the DLT instead of blocking or being dropped.
- Consumers recover from transient failures automatically.

### Negative
- A poison message occupies retry cycles before landing in the DLT.

### Risks
- **Risk**: DLT grows unbounded — **Mitigation**: SLO alert + runbook.

## Related Decisions

- ADR-005 (Kafka backbone)
- ADR-008 (idempotency)

## References

- `docs/architecture/retry-dlt.md`
- `DeadLetterTopicIT`
- `docs/runbooks/dlt-inspection.md`
