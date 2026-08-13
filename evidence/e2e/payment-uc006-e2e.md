# Aegis — E2E Evidence: UC-006 Execute Payment

> **Date:** 2026-08-12 — **Tool:** Playwright (chromium) — **Sandbox:** docker-compose stack
> **Command:** `npx playwright test` (in `e2e/`)

## Result

**14/14 passed** — the 3 new UC-006 payment scenarios plus the existing auth,
wallet, transfer API and transfer UI scenarios (no regressions).

### Payments — UC-006 saga (API-level) — 3 passed

| Scenario | Verdict |
|----------|---------|
| Executes a payment to a merchant: fraud → hold → atomic debit | ✅ PASS |
| Rejects duplicate payment reference (idempotency → 409) | ✅ PASS |
| Fails closed when the wallet has insufficient funds (422) | ✅ PASS |

The payment spec drives the **full saga end-to-end** against the live stack:
identity register → wallet → deposit → `POST /api/v1/payments` (payment service,
`X-User-Id` header) → asserts `COMPLETED`, holds balance invariance
(wallet − amount), duplicate reference → 409 with no double debit, insufficient
funds → 422 `INSUFFICIENT_FUNDS`.

Raw output: `results.json` (Playwright JSON reporter), `../html-report/` (HTML).

## Bug found and fixed by this E2E run

The payment controller originally accepted `userId` from the **request body** and
required it — a caller could pay from another user's wallet, and the body
contract (OpenAPI spec 006) never had `userId`. Fixed to derive the user from the
`X-User-Id` header (BFF-populated from the session JWT), matching the contract
and the wallet service pattern. E2E confirms the header-authenticated flow works.
