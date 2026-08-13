# Aegis — E2E Evidence: UC-007 Refund Payment

> **Date:** 2026-08-13 — **Tool:** Playwright (chromium) — **Sandbox:** docker-compose stack
> **Command:** `npx playwright test` (in `e2e/`)

## Result

**19/19 passed** — the 5 new UC-007 refund scenarios plus the existing auth,
wallet, transfer (API + UI) and payment scenarios (no regressions).

### Refunds — UC-007 saga (API-level) — 5 passed

| Scenario | Verdict |
|----------|---------|
| Refunds a completed payment in full: balance restored | ✅ PASS |
| Rejects a duplicate refund reference (idempotent → same refund, no double credit) | ✅ PASS |
| Rejects refunding an already-refunded payment (409) | ✅ PASS |
| Rejects a refund that exceeds the payment amount (422) | ✅ PASS |
| Rejects refunding a payment owned by another user (403) | ✅ PASS |

The refund spec drives the **full saga end-to-end**: register → wallet → deposit →
payment (`COMPLETED`) → `POST /api/v1/payments/{id}/refund` (`X-User-Id`) →
asserts `COMPLETED` refund, **balance restored** (`newBalance`), duplicate
reference returns the same refund with no double credit, already-refunded → 409,
exceeds → 422, cross-user → 403.

Raw output: `results.json` (Playwright JSON reporter), `../html-report/` (HTML).

## Bugs found and fixed by this E2E run

Booting the sandbox surfaced two defects unit tests could not catch (see
`evidence/load/RESULTS.md` finding #8):

1. `PaymentJpaRepository.findByIdForUpdate` broke the payment service at startup
   (`No property 'forUpdate' found`) — added the missing explicit `@Query`.
2. `RefundResponse.newBalance` was null — the use case now returns a `RefundResult`
   carrying the post-credit wallet balance.
