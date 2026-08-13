# ADR-016: Refunds are New Ledger Credits, Not Settlements

## Status

Accepted

## Date

2026-08-13

## Context

UC-007 (Refund Payment) reverses a completed payment. The naive approach is to
"undo" the original wallet debit — but the ledger is immutable (ADR-004): entries
are append-only, and corrections are new movements. A refund must therefore
**credit** the payer's wallet with a new `REFUND` ledger entry, not delete or
negate the original payment debit.

Forces:

- Ledger immutability: `balance == SUM(ledger)` must hold at all times; a refund
  is a new positive entry that restores the invariant.
- The payment `PaymentStatus.REFUNDED` already exists (UC-006 reserved it).
- The wallet `LedgerEntryType.REFUND` already exists (UC-006 pre-defined).
- Refunds must be idempotent (duplicate refund references never double-credit)
  and concurrency-safe (two concurrent refunds of the same payment must not both
  succeed).

## Decision

**Model a refund as a new `Refund` aggregate in the Payment Service that credits
the payer's wallet with a `REFUND` ledger entry, then transitions the payment
`COMPLETED → REFUNDED`.**

Concretely:

1. New domain: `Refund` aggregate, `RefundStatus`
   (`PENDING → COMPLETED | FAILED`), and the `PaymentRefunded` domain event on
   topic `payment.refunded`.
2. New inbound port `RefundPaymentUseCase` and application service, mirroring the
   payment saga structure: validate → persist PENDING + publish → credit wallet →
   complete → publish `PaymentRefunded`. On credit failure → `FAILED` +
   `PaymentRefunded` (failed) or compensation as appropriate.
3. Wallet Service gains a **refund-credit endpoint** that increases the balance
   and appends a `REFUND` ledger entry (reference = refund id) in one ACID
   transaction, idempotent by reference. This mirrors the UC-006 debit-only
   `debit` endpoint in reverse.
4. Validation: payment is `COMPLETED`, owned by the requester (or admin
   override), refund amount ≤ original payment amount; duplicate refund
   references are idempotent.
5. Single refund per payment: the payment becomes `REFUNDED` on the first
   successful refund (multiple partial refunds are a future extension).

## Alternatives Considered

### Alternative 1: Reverse the original ledger entry (mark as reversed)
- **Pros**: balance stays identical to "no payment happened".
- **Cons**: violates ledger immutability (ADR-004); makes the original payment
  entry non-representative of the settled event; complicates reporting (the
  payment still happened, then was refunded).
- **Why not chosen**: a refund is a new financial event, not an erasure.

### Alternative 2: Use the transfer settle with source==dest (round-trip)
- **Pros**: reuses the settle endpoint.
- **Cons**: settle requires source ≠ dest (UC-005 invariant) and would create
  TRANSFER_OUT/IN pairs, mislabelling a refund; no REFUND ledger type.
- **Why not chosen**: a refund is not a transfer; the ledger type must be REFUND.

### Alternative 3: Refund orchestrated entirely in the Wallet Service
- **Pros**: credit already lives in the wallet.
- **Cons**: same bounded-context objection as ADR-014/015; the payment lifecycle
  and refund ownership belong to the Payment Service.
- **Why not chosen**: consistency with the existing saga ownership.

## Consequences

### Positive
- Ledger invariant preserved (refund = new REFUND credit entry).
- Idempotent and concurrency-safe by payment lock + refund reference.
- Reuses the outbox, envelope, and audit patterns; generalizes naturally.
- Complete the payments story: money in (deposit), out (transfer/payment), back
  (refund).

### Negative
- Wallet balance reflects the payment AND the refund (two entries) — accepted;
  it is the correct ledger representation.
- Single-refund-per-payment limits partial-refund accumulation for now.

### Risks
- **Risk**: concurrent refund double-credit — **Mitigation**: pessimistic lock on
  the payment row during the refund saga.
- **Risk**: wallet not ACTIVE at refund time — **Mitigation**: validate wallet
  state in the credit endpoint (`WALLET_NOT_ACTIVE`).
- **Risk**: duplicate refund retries — **Mitigation**: idempotent credit by
  refund reference.

## Related Decisions

- ADR-004 (single-entry, immutable ledger; REFUND is a new entry)
- ADR-006 (outbox), ADR-008 (idempotency), ADR-009 (envelope), ADR-010 (retry/DLT)
- ADR-014/015 (saga mechanics reused)

## References

- `specs/007-refund-payment/spec.md`
- `specs/007-refund-payment/contracts/api/refund-api.yaml`
- `specs/007-refund-payment/contracts/events/payment-refunded.yaml`
- GitHub: epic #246, UC-007 #10
