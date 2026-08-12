# ADR-015: Merchant Payments Reuse the Transfer Saga Mechanics

## Status

Accepted

## Date

2026-08-12

## Context

UC-006 (Execute Payment) debits a user's wallet to pay a merchant/payee. It shares
almost every moving part with UC-005 (Transfer Funds): a synchronous fraud
decision before any fund movement, a fund reservation (hold) during processing,
an atomic wallet debit on success, and compensation (hold release) on failure.
The UC-005 saga (ADR-014) already delivers these mechanics:

- Fraud assessment via `POST /api/v1/fraud/assess` (fail-closed)
- Hold creation via `POST /api/v1/wallets/{id}/holds`
- Atomic settlement via `POST /api/v1/wallets/transfers/settle`
- Hold release via `POST /api/v1/wallets/{id}/holds/{holdId}/release`
- Transactional outbox publishing, retry/DLT, idempotency keys

The question is how to model a payment without duplicating or forking this
machinery.

Forces:

- The transfer saga is battle-tested (E2E green, p95 ~71 ms) — rewriting or
  forking it for payments risks a parallel, divergent codebase.
- A payment differs from a transfer in *shape* (one wallet, a `payee` VO, its own
  status machine and events) but not in *integrity mechanics* (fraud → hold →
  atomic debit → compensation).
- Payments need their own events (`payment.requested/executed/failed`) and their
  own read model (payment history), because a payment is not a transfer.

## Decision

**Model payments as a new `Payment` aggregate in the Payment Service that
orchestrates the *same* saga steps (fraud → hold → settle) already implemented
for transfers — without sharing the `Transfer` aggregate or its state machine.**

Concretely:

1. New domain: `Payment` aggregate, `PaymentStatus`
   (`PENDING → PROCESSING → COMPLETED | FAILED`, `REFUNDED` reserved for UC-007),
   `Payee` value object, and domain events `PaymentRequested`/`PaymentExecuted`/
   `PaymentFailed` on topics `payment.requested|executed|failed`.
2. New inbound port `ExecutePaymentUseCase` and application service
   `ExecutePaymentService`, mirroring the two-phase transaction structure of
   `TransferService` (persist + publish, then fraud check → hold → settle →
   complete with compensation).
3. Reuse the **outbound ports and adapters unchanged**: `FraudAssessmentGateway`
   (`RestFraudAssessmentGateway`) and `WalletGateway` (`RestWalletGateway`). The
   wallet service is agnostic to whether the settlement reference belongs to a
   transfer or a payment — it keys ledger entries by the reference.
4. The scheduled hold-expiry job remains shared (it releases holds and
   reconciles the owning aggregate to `FAILED`; for payments it reconciles the
   payment).

## Alternatives Considered

### Alternative 1: Generalize `Transfer` into a `MoneyMovement` supertype
- **Pros**: one aggregate, one saga, one event set.
- **Cons**: conflates two distinct bounded-context shapes (transfers move money
  between wallets; payments move money to a payee). Couples the transfer's
  evolution (e.g. refunds, UC-007) with payments; makes event contracts
  ambiguous; complicates the state machine.
- **Why not chosen**: the aggregates are both "money out of a wallet" but their
  lifecycle and events differ enough that separate aggregates with shared
  *mechanics* (not shared state) is the cleaner DDD boundary.

### Alternative 2: Payment orchestrated entirely inside the Wallet Service
- **Pros**: debit already lives in the wallet; one less hop.
- **Cons**: same bounded-context objection as ADR-014 (payment lifecycle,
  fraud orchestration and future refunds belong to the Payment Service); would
  fork the saga machinery into the wallet.
- **Why not chosen**: the Payment Service is the home of UC-005/006/007.

### Alternative 3: Reuse `Transfer` with a payee-like destination wallet
- **Pros**: zero new aggregate.
- **Cons**: semantically wrong (no merchant model, no payment events, payment
  history indistinguishable from transfers); would leak payment concerns into
  transfers.
- **Why not chosen**: a payment is not a transfer to a synthetic wallet.

## Consequences

### Positive
- UC-006 reuses the proven, tested saga machinery end to end — fraud gateway,
  wallet gateway, outbox, retry/DLT, idempotency — no parallel code.
- Payments get their own aggregate, state machine, events and read model.
- Directly generalizes to UC-007 (refunds: `COMPLETED → REFUNDED` with a
  compensating ledger entry) reusing the same mechanics.

### Negative
- Two aggregates with similar orchestration code in the same service (accepted:
  the shared parts are the adapters/ports, the aggregate logic is distinct).
- The wallet `settle` semantics ("consume a hold") now serve two callers; the
  reference field must disambiguate (transfer id vs payment id) — satisfied by
  idempotency keying already in place.

### Risks
- **Risk**: parallel code drift between transfer and payment sagas —
  **Mitigation**: extract shared orchestration helpers if they grow; ADR
  documents the shared adapter boundary.
- **Risk**: fraud decision semantics for `transactionType=PAYMENT` —
  **Mitigation**: the fraud service already accepts a transaction type; UC-006
  passes `PAYMENT`.

## Related Decisions

- ADR-014 (saga orchestration with fund reservation) — reused unchanged
- ADR-004 (single-entry ledger), ADR-006 (outbox), ADR-008 (idempotency),
  ADR-009 (envelope), ADR-010 (retry/DLT)

## References

- `specs/006-execute-payment/spec.md`
- `specs/006-execute-payment/contracts/api/payments-api.yaml`
- `specs/006-execute-payment/contracts/events/`
- GitHub: epic #246, UC-006 #9
