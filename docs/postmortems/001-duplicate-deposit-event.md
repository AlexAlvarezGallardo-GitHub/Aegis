# Postmortem 001 — Duplicate Deposit Event

> **Status:** Closed — resolved by idempotency hardening (Sprint 3).
> **Type:** Simulated incident for the reference architecture.

## Summary

| Field | Value |
|-------|-------|
| **Incident ID** | INC-001 |
| **Severity** | P1 (financial integrity) |
| **Start** | 2026-08-05 09:14 UTC |
| **End** | 2026-08-05 09:41 UTC |
| **Duration** | 27 minutes |
| **Impact** | A wallet's balance was credited twice for a single deposit request |
| **Detection** | Reconciliation drift + duplicate ledger entry in audit |

## Timeline

| Time (UTC) | Event |
|------------|-------|
| 09:14 | User submits a deposit; network timeout on the client; client retries |
| 09:14 | Both requests reach `DepositFundsService` concurrently with the same `reference` |
| 09:15 | Both pass the in-memory duplicate check (race) |
| 09:15 | Two `DEPOSIT` ledger entries + two `FUNDS_DEPOSITED` outbox events committed |
| 09:16 | Wallet balance credited twice (+200 instead of +100) |
| 09:30 | Reconciliation job (manual run) flags the wallet: balance ≠ ledger sum |
| 09:34 | Operator inspects the ledger and identifies the duplicate deposit |
| 09:41 | Correction applied: compensating `REVERSAL` entry; balance restored |

## Root cause

**Race condition in the idempotency check.** `DepositFundsService` validated
duplicates by scanning the wallet's in-memory ledger entries. Two concurrent
requests with the same `reference` could both pass that check before either wrote
to the database, so the unique constraint (which did not exist at the time) never
fired.

Contributing factors:

- No unique constraint on `(wallet_id, reference)` for deposits.
- No `@Lock`/pessimistic write on the aggregate for deposit processing.
- Client retries without a stable idempotency key being enforced server-side.

## Resolution

1. **Application**: `DepositFundsService` still performs the fast in-memory check.
2. **Database**: unique partial index `idx_ledger_entries_deposit_reference` on
   `(wallet_id, reference)` for `type='DEPOSIT' AND reference IS NOT NULL`
   (migration V3). A concurrent duplicate now fails at the DB and is translated to
   `DuplicateDepositException` (HTTP 409).
3. **Compensation**: corrections are applied as immutable `REVERSAL` entries
   (ADR-004); the original deposit entry is never edited.
4. **Verification**: `ConcurrentDepositIdempotencyIT` fires N concurrent deposits
   with the same reference and asserts exactly one succeeds.

## Impact

- 1 user-visible financial error.
- No data loss; corrected within the incident window.
- No external customers affected (reference architecture).

## Actions

| Action | Owner | Status |
|--------|-------|--------|
| Add unique deposit reference index | Wallet | Done (Sprint 3) |
| Translate concurrent duplicate to 409 | Wallet | Done (Sprint 3) |
| Add concurrency IT | Wallet | Done (Sprint 3) |
| Add compensation (reversal) flow | Wallet | Done (Sprint 4) |
| Add reconciliation job with drift metric | Wallet | Done (Sprint 4) |
| Document the pattern | Architecture | Done |

## Lessons learned

- **Never rely on an in-memory check alone** for financial idempotency — the DB
  constraint is the source of truth.
- **At-least-once delivery + retries require idempotent consumers and idempotent
  operations.** Both are now enforced.
- **Compensations keep the audit trail immutable.** Editing history would destroy
  the evidence needed for the postmortem.
- **Reconciliation is the safety net** that catches what the application and
  database checks miss.

## See also

- [Idempotency](../architecture/idempotency.md)
- [ADR-004: Ledger design](../adr/ADR-004-ledger-single-entry.md)
- [Reconciliation](../architecture/reconciliation.md)
- [Compensations](../architecture/compensations.md)
