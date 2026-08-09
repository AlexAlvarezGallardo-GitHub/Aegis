# Idempotency in Aegis

> **Status:** Implemented for deposits. Other operations (wallet creation, balance
> adjustment) rely on unique identifiers rather than idempotency keys.

## Definition

An operation is **idempotent** when repeating it with the same inputs produces
the same outcome and does not create side effects more than once. In Aegis this is
needed because:

- Clients retry failed HTTP requests (timeouts, 5xx).
- Kafka delivers **at-least-once**; a consumer may see an event twice.

## Scope

| Operation | Idempotency mechanism | Status |
|-----------|----------------------|--------|
| Create wallet | Client-generated `correlationId`; no financial effect | Deterministic on retry |
| Deposit funds | Client-supplied `reference` idempotency key | **Implemented** |
| Adjust balance | No key (operator-triggered) | Not applicable |
| Consumer processing | `eventId` deduplication in `processed_events` | **Implemented** |

## Deposit idempotency

### Contract

`POST /api/v1/wallets/{walletId}/deposits` requires a `reference` field. The caller
generates a stable reference (UUID or payment-provider reference) and reuses it on
retries.

### Two-layer defense

1. **Application check** (`DepositFundsService.deposit`): loads the wallet with its
   ledger entries and rejects the request with `DuplicateDepositException` (HTTP 409)
   if the reference already exists.

2. **Database constraint**: unique partial index
   `idx_ledger_entries_deposit_reference`:

   ```sql
   CREATE UNIQUE INDEX idx_ledger_entries_deposit_reference
       ON ledger_entries (wallet_id, reference)
       WHERE type = 'DEPOSIT' AND reference IS NOT NULL;
   ```

   This closes the race between two concurrent requests that both pass the
   in-memory check. The second insert violates the constraint and the service
   translates the integrity violation to `DuplicateDepositException`.

### Concurrency behaviour

The integration test `ConcurrentDepositIdempotencyIT` fires N concurrent deposits
with the same reference and asserts exactly one succeeds and the rest are rejected
as duplicates. Because both the in-memory check and the DB write run inside the
same transaction, and the index is enforced at commit time, the outcome is
deterministic.

### Persistence and retention

- Ledger entries are never deleted (financial immutability).
- The idempotency record lives for the lifetime of the wallet (no retention/expiry).
- The reference is stored in `ledger_entries.reference` (`VARCHAR(255)`).

## Consumer deduplication

Each consumer maintains a `processed_events` table:

| Column | Type | Notes |
|--------|------|-------|
| `event_id` | UUID (PK) | Unique event identifier |
| `topic` | VARCHAR(255) | Source topic |
| `partition` | INTEGER | Source partition |
| `offset` | BIGINT | Source offset |
| `processed_at` | TIMESTAMP | When processed |

The consumer registers the event with an atomic `INSERT ... ON CONFLICT DO NOTHING`
(`ProcessedEventJpaRepository.insertIfAbsent`) before applying it. A returned count
of `0` means the event was already processed and the consumer skips it.

This is the **recommended deduplication pattern** for at-least-once delivery: the
application logic remains idempotent at the boundary without relying on Kafka
exactly-once semantics.

## Trade-offs and alternatives considered

- **Exactly-once (Kafka transactions):** more complex, requires transaction-aware
  producers and consumers; not needed given the idempotent consumers.
- **Processing table with `ON CONFLICT`:** chosen — simple, portable, testable.
- **Application-side in-memory dedup:** insufficient for multi-instance; rejected.

## See also

- [Deposit Flow](sequences/deposit-flow.md)
- [Retries and Dead Letter Topics](retry-dlt.md)
- ADR on the transactional outbox (ADR-002)
