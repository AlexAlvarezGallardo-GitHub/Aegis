# ADR-004: Ledger Design — Single-Entry Per-Aggregate Ledger

## Status

Accepted

## Date

2026-08-07

## Context

The Wallet Service tracks a customer's balance and records every financial
movement. Today the ledger is implemented as a flat list of `LedgerEntry` rows on
the `Wallet` aggregate (`ledger_entries` table), with a single entry per operation
(deposit, withdrawal, etc.). Each entry stores an absolute, non-negative `amount`
and a `LedgerEntryType`.

As the platform grows, we must decide whether this single-entry model is the right
choice or whether we need double-entry bookkeeping (`Settlement account` /
`Customer wallet`, zero-sum invariants, compensations).

Constraints and forces:
- The reference architecture must demonstrate a **defensible financial model**
  without over-engineering the current scope (deposits + balance adjustments).
- Ledger entries must be **immutable** (financial auditability).
- The unique deposit idempotency index (V3) already guarantees a deposit reference
  is applied at most once per wallet.
- No real regulatory or external-provider integration exists yet; PRE/STAGE/PROD are
  prepared but not operating.
- We want to keep the domain model simple and testable while leaving room for a
  future double-entry ledger.

## Decision

**Keep a single-entry, per-aggregate ledger with immutability and compensations.**

- The ledger remains a chronological, immutable list of `LedgerEntry` rows per
  wallet.
- Every mutation is **append-only**: corrections and reversals are recorded as new
  compensating entries, never by editing an existing entry.
- Monetary amounts are `BigDecimal` with scale 2 (minor units rounding), and entries
  never store negative amounts; the sign is encoded in the `LedgerEntryType`
  (`WITHDRAWAL`, `TRANSFER_OUT`, `PAYMENT` represent outflows).
- A compensating entry type (`REVERSAL`/`REFUND`) references the original entry id
  so the audit trail preserves the full history.
- The running `balance` on `Wallet` is a **derived projection** maintained eagerly;
  it must equal `SUM(inflows) - SUM(outflows)` over ledger entries. This invariant is
  verified by the reconciliation job (see ADR on reconciliation).
- **Double-entry is explicitly deferred.** The platform does not yet operate a
  settlement account, inter-account transfers, or a central bank integration, so a
  full double-entry ledger would add complexity without a consumer for the second
  leg.

## Alternatives Considered

### Alternative 1: Full double-entry ledger (Settlement account + Customer wallet)
- **Description**: Every movement writes two entries (debit/credit) across two
  accounts, enforcing zero-sum invariants.
- **Pros**: Strong financial guarantees, standard for banking, supports inter-account
  transfers and reconciliation against a settlement ledger.
- **Cons**: Doubles row count and complexity; requires an internal transfer domain
  that does not exist yet; heavier domain model and tests.
- **Why not chosen**: The current scope (single customer wallet, deposits and
  adjustments) has no second leg to post to. Introducing it now would be speculative.

### Alternative 2: Single entry with signed amounts
- **Description**: Keep one entry per operation but store signed `BigDecimal`
  amounts (positive/negative) and derive the balance by summing.
- **Pros**: Simpler balance computation; fewer enum types.
- **Cons**: Invites sign confusion in business logic; less explicit about intent;
  idempotency and compensation checks become harder to reason about.
- **Why not chosen**: Explicit entry types are self-documenting and reduce the risk
  of sign bugs in a financial domain.

### Alternative 3: Event-sourced ledger (ledger as events)
- **Description**: Rebuild ledger state purely from a domain event stream.
- **Pros**: Full auditability; trivial replay; aligns with the Kafka backbone.
- **Cons**: Requires event sourcing infrastructure, snapshotting, and changes to the
  entire persistence model; out of scope for a reference architecture today.
- **Why not chosen**: The transactional outbox already gives at-least-once event
  delivery; a state-based ledger with immutable entries is simpler and sufficient.

## Consequences

### Positive
- The audit trail is immutable and complete: corrections never erase history.
- The domain model stays simple, testable, and aligned with the current scope.
- Compensations (reversals/refunds) are first-class ledger movements, ready for
  reversal flows.
- The derived-balance invariant can be continuously checked by the reconciliation
  job.

### Negative
- No built-in zero-sum guarantee across accounts (a programming error could corrupt
  the balance; mitigated by reconciliation).
- A future move to double-entry will require a migration of the ledger schema.

### Risks
- **Risk**: Balance and ledger sum drift — **Mitigation**: reconciliation job that
  recomputes the balance from entries and alerts on mismatch.
- **Risk**: Compensating entries created without a reference to the original —
  **Mitigation**: `reference`/`reversalOf` linkage enforced by the domain model.
- **Risk**: Entries edited after the fact — **Mitigation**: entries are immutable
  records; persistence layer exposes no update path.

## Related Decisions

- ADR-002: Kafka topic configuration (events flow from ledger movements)
- V3 migration: unique deposit reference index (idempotency)
- ADR on reconciliation (planned): derived-balance invariant checking

## References

- `backend/aegis-wallet-service/src/main/java/com/aegis/wallet/domain/model/LedgerEntry.java`
- `backend/aegis-wallet-service/src/main/java/com/aegis/wallet/domain/model/LedgerEntryType.java`
- `specs/004-deposit-funds/spec.md`
- `docs/architecture/idempotency.md`
