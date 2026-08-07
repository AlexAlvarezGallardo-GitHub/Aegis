# ADR-009: Event Versioning and Standard Envelope

## Status

Accepted

## Date

2026-08-07

## Context

Events evolve (additive fields, schema fixes) and producers/consumers deploy
independently. Without a versioning strategy, a producer change can break
consumers, and there is no standard for the metadata every event carries.

## Decision

**Every domain event uses a standard envelope and a semantic schema version.**

Envelope fields:

| Field | Required | Purpose |
|-------|----------|---------|
| `eventId` | yes | Global unique id (deduplication) |
| `eventType` | yes | Type discriminator |
| `schemaVersion` | yes | Semver of the payload schema |
| `occurredAt` | yes | When the event occurred (alias `timestamp`) |
| `correlationId` | yes | End-to-end correlation |
| `causationId` | optional | Event/command that caused this event |
| `aggregateId` / `aggregateType` | yes | Aggregate context |

Versioning rules:
- Additive/relaxing changes bump the **minor** (`1.0` → `1.1`), consumers keep
  working.
- Breaking changes (field removal, type change) bump the **major** (`1.0` → `2.0`)
  and require coordinated rollout.
- Consumers deserialize tolerantly (ignore unknown fields).

## Alternatives Considered

### Alternative 1: Schema registry (Confluent Avro/Protobuf)
- **Pros**: enforced compatibility.
- **Cons**: extra infrastructure; JSON + tolerant consumers are sufficient now.

### Alternative 2: Unversioned JSON
- **Pros**: zero ceremony.
- **Cons**: no way to know what a consumer can parse; breakage on rename.

**Why not chosen**: explicit `schemaVersion` + tolerant deserialization balances
safety and simplicity for the current event set.

## Consequences

### Positive
- Safe independent deployment of producers/consumers.
- Deduplication key (`eventId`) is standard across all events.

### Negative
- Teams must remember to bump the version on breaking changes.

### Risks
- **Risk**: version not bumped — **Mitigation**: schema contracts + review.

## Related Decisions

- ADR-005 (Kafka backbone)
- ADR-008 (idempotency)

## References

- `docs/architecture/event-versioning.md`
- `specs/*/contracts/events/*.yaml`
