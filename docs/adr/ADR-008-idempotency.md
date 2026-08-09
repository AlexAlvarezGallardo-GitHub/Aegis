# ADR-008: Idempotency Strategy

## Status

Accepted

## Date

2026-08-07

## Context

Deposits must be idempotent: a client retry (network timeout) or a Kafka redelivery
must not apply the same financial operation twice. Without this, a single deposit
could credit a wallet multiple times (see postmortem 001).

## Decision

**Idempotency is enforced at two layers.**

1. **Application check**: `DepositFundsService` scans the wallet's ledger entries
   for the client-supplied `reference` and rejects duplicates with
   `DuplicateDepositException` (HTTP 409).
2. **Database constraint**: a unique partial index
   `idx_ledger_entries_deposit_reference` on `(wallet_id, reference)` for
   `type='DEPOSIT' AND reference IS NOT NULL` closes the race between concurrent
   requests that both pass the in-memory check.

**Consumers** maintain a `processed_events` table keyed by `eventId` and use
`INSERT ... ON CONFLICT DO NOTHING` so redeliveries are no-ops.

## Alternatives Considered

### Alternative 1: In-memory check only
- **Pros**: simplest.
- **Cons**: not safe under concurrency (the postmortem root cause).

### Alternative 2: Kafka exactly-once semantics
- **Pros**: removes consumer-level dedup burden.
- **Cons**: complex; needs transactional producers/consumers; not required given
  idempotent consumers.

### Alternative 3: Distributed lock (Redis)
- **Pros**: central.
- **Cons**: extra infrastructure; DB unique index is simpler and stronger.

**Why not chosen**: the DB unique index is the source of truth and provides
exactly-once apply semantics at the persistence layer without extra infra.

## Consequences

### Positive
- Concurrent duplicates rejected deterministically.
- Consumers safe under at-least-once delivery.

### Negative
- A DB constraint failure must be translated to a domain exception.

### Risks
- **Risk**: reference collisions across different sources — **Mitigation**: unique
  per wallet, documented reference guidance.

## Related Decisions

- ADR-004 (ledger)
- ADR-006 (outbox)
- ADR-005 (Kafka)

## References

- `docs/architecture/idempotency.md`
- `ConcurrentDepositIdempotencyIT`
- `docs/postmortems/001-duplicate-deposit-event.md`
