# Aegis — E2E Evidence: UC-005 Transfer Funds

> **Date:** 2026-08-11 — **Tool:** Playwright (chromium) — **Sandbox:** docker-compose stack
> **Command:** `npx playwright test` (in `e2e/`)

## Result

**10/10 passed** — the 4 new UC-005 transfer scenarios plus the 6 existing auth/wallet
scenarios (no regressions).

### Transfers — UC-005 saga (API-level) — 4 passed

| Scenario | Verdict |
|----------|---------|
| Transfers funds between wallets: fraud → hold → atomic settle | ✅ PASS |
| Rejects duplicate transfer reference (idempotency → 409) | ✅ PASS |
| Fails closed when source has insufficient funds (422) | ✅ PASS |
| Rejects self-transfer (same source and destination) | ✅ PASS |

The transfer spec drives the **full saga end-to-end** against the live stack:
identity register → 2 wallets → deposits → `POST /api/v1/transfers` (payment
service) → asserts `COMPLETED`, holds balance invariance (source − amount,
destination + amount), duplicate reference → 409 with no double debit,
insufficient funds → 422 `INSUFFICIENT_FUNDS`, self-transfer rejected.

Raw output: `results.json` (Playwright JSON reporter), `../html-report/` (HTML).

## Bugs found and fixed by this E2E run

Booting a clean sandbox exposed four defects the unit tests could not catch (see
`evidence/load/RESULTS.md` findings #6):

1. Reserved SQL keyword `offset` in `processed_events` migrations (payment, fraud,
   reporting) — Flyway failed at boot.
2. `KafkaTemplate<String, String>` vs `<String, Object>` bean mismatch in
   `OutboxRelayScheduler` (payment, fraud) — services failed to start.
3. Saga orchestrator incomplete — transfer stopped at `FRAUD_CHECK`; implemented
   `RestWalletGateway` + fraud → hold → settle → `COMPLETED` with compensation.
4. Payment → wallet/fraud base URLs defaulted to `localhost` inside the container.
