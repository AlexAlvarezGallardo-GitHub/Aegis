# ADR-014: Orchestrated Saga with Fund Reservation for Cross-Wallet Transfers

## Status

Accepted

## Date

2026-08-10

## Context

UC-005 (Transfer Funds) moves money between two wallets. The operation spans
three bounded contexts:

1. **Payment Service** (new) owns the `Transfer` aggregate and its lifecycle.
2. **Fraud Service** must authorize the movement *before* any funds move
   (fail-closed; UC-008 decision thresholds).
3. **Wallet Service** must debit the source wallet and credit the destination
   wallet **atomically** — partial settlement is never acceptable.

Constraints and forces:

- There are no distributed transactions across services (Kafka + REST; ADR-005).
- The fraud decision is inherently **synchronous**: the transfer cannot proceed
  without it, and the caller waits for the outcome.
- Both wallets live in the *same* Wallet Service database, so the actual
  debit+credit can commit in one local ACID transaction.
- The orchestrator can crash between steps — reserved funds must not lock
  forever, and every terminal state must be recoverable and auditable.
- Ledger entries are immutable; corrections are compensations (ADR-004).

## Decision

**Use an orchestrated saga in the Payment Service, with a fund-reservation
(hold) mechanism in the Wallet Service.**

Steps of the saga (synchronous REST from the orchestrator):

1. Persist `Transfer` (`PENDING`), publish `TransferRequested` via outbox.
2. Call Fraud Service (`POST /api/v1/fraud/assess`). `REJECT` or unavailability
   → `FAILED`, publish `TransferFailed` (fail-closed).
3. Reserve funds: `POST /api/v1/wallets/{id}/holds` — an `ACTIVE` hold reduces
   *available* balance (`balance − Σ active holds`) without moving money.
   Insufficient funds → `FAILED`.
4. Settle: `POST /api/v1/wallets/transfers/settle` — in **one** database
   transaction the Wallet Service settles the hold, debits the source
   (`TRANSFER_OUT`) and credits the destination (`TRANSFER_IN`), and writes the
   outbox event(s). Transfer → `COMPLETED`, publish `TransferCompleted`.
5. Any failure after reservation → **compensation**: release the hold
   (`POST .../holds/{holdId}/release`), transfer → `FAILED`, publish
   `TransferFailed` with `compensated=true`.

Supporting mechanics:

- **Hold TTL (5 min)** + scheduled job: holds orphaned by an orchestrator crash
  expire and are released; their transfers reconcile to `FAILED`
  (`HOLD_EXPIRED`).
- **Idempotency end to end**: client `reference` unique per source wallet;
  hold keyed by transfer id; settlement re-submission returns the original
  result; consumers deduplicate by `eventId` (ADR-008).
- **Deadlock avoidance**: settlement locks wallet rows in deterministic
  (id-ordered) sequence.
- **Kafka carries facts, not commands**: events notify downstream (audit,
  reporting, fraud signal); saga steps are synchronous calls.

## Alternatives Considered

### Alternative 1: Two-Phase Commit / XA across services
- **Pros**: strong atomicity guarantee in theory.
- **Cons**: no XA span across REST + Kafka; holds database locks across network
  calls; couples availability of all participants; not supported by the stack.
- **Why not chosen**: operationally fragile and incompatible with the
  event-driven backbone.

### Alternative 2: Choreographed saga via Kafka (event replies)
- **Pros**: fully decoupled; no central orchestrator.
- **Cons**: the fraud decision must be synchronous anyway (the caller waits);
  compensations become event cascades that are harder to trace and test;
  transfer status polling/async UX for the client.
- **Why not chosen**: the use case is request/response in nature. Choreography
  trades a simple synchronous flow for distributed state-guessing.

### Alternative 3: Transfer logic inside Wallet Service (no Payment Service)
- **Pros**: one fewer service; debit/credit already local.
- **Cons**: mixes the payment bounded context (transfer lifecycle, fraud
  orchestration, future merchant payments/refunds UC-006/UC-007) into the
  wallet context; fraud orchestration would live in the wrong service;
  contradicts the platform roadmap (Payment Service is planned).
- **Why not chosen**: bounded-context ownership matters more than saving one
  deployment unit. The Payment Service is the home of UC-005/006/007.

### Alternative 4: Debit-then-credit without holds (best-effort compensation)
- **Pros**: simpler; no hold entity.
- **Cons**: a debit commits before knowing the credit succeeds; compensation is
  a *new money movement* (re-credit) with its own failure modes; balance
  invariants are violated in between.
- **Why not chosen**: holds keep money unmoved until settlement is guaranteed;
  compensation is a state transition, not a financial operation.

## Consequences

### Positive
- No partial settlement: debit+credit commit atomically in one local
  transaction; the saga only orchestrates around that atomic core.
- Fail-closed fraud posture: no funds move (not even into a hold) without an
  explicit APPROVE/REVIEW.
- Crash-safe: hold TTL + release job guarantee funds are never locked forever;
  every transfer reaches a terminal, auditable state.
- The pattern generalizes directly to UC-006 (merchant payments) and UC-007
  (refunds as compensating movements).
- Demonstrates production-grade distributed-transaction design (saga,
  reservation, compensation, idempotency) with the existing stack.

### Negative
- Two extra RPCs per transfer (hold + settle) and a hold table to maintain.
- Synchronous chain (payment → fraud → wallet) adds latency; mitigated by the
  fraud 200ms budget and circuit breakers.
- Expiry/reconciliation jobs must be operated and monitored.

### Risks
- **Risk**: orchestrator crash between hold and settle — **Mitigation**: hold
  TTL + scheduled release job; transfer reconciled to `FAILED`.
- **Risk**: fraud/wallet latency degrades transfer UX — **Mitigation**:
  circuit breakers, timeouts, `FRAUD_UNAVAILABLE` fail-closed response,
  OpenTelemetry tracing across the saga (ADR-012).
- **Risk**: expired-but-not-yet-released hold settled concurrently —
  **Mitigation**: settlement validates hold state under lock; deterministic
  `409 HOLD_NOT_ACTIVE` triggers compensation.
- **Risk**: duplicate client retries — **Mitigation**: unique `reference` per
  source wallet (409) + idempotent hold/settle (ADR-008).

## Related Decisions

- ADR-004 (single-entry ledger; TRANSFER_OUT/TRANSFER_IN + compensations)
- ADR-005 (Kafka backbone) — events, not commands
- ADR-006 (transactional outbox)
- ADR-008 (idempotency strategy)
- ADR-009 (event envelope/versioning)
- ADR-010 (retry/DLT)
- ADR-011 (BFF edge for the SPA)

## References

- `specs/005-transfer-funds/spec.md`
- `specs/005-transfer-funds/contracts/api/transfer-api.yaml`
- `specs/005-transfer-funds/contracts/api/wallet-transfer-api.yaml`
- `specs/005-transfer-funds/contracts/events/`
- GitHub: epic #246, UC-005 #8, tasks #247–#253
