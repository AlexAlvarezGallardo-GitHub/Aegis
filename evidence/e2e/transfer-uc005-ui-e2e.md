# Aegis — E2E Evidence: UC-005 Transfer Funds (UI)

> **Date:** 2026-08-11 — **Tool:** Playwright (chromium) — **Sandbox:** docker-compose stack
> **Command:** `npx playwright test` (in `e2e/`)

## Result

**7/7 passed** (auth 2, wallet 4, transfer UI 1) — no regressions.

### Transfers — UC-005 UI (new in PR #263) — 1 passed

| Scenario | Verdict |
|----------|---------|
| Transfers funds between wallets via the wallet detail page | ✅ PASS |

Drives the real browser UI: login → wallet detail → **Transfer** button opens
the new `TransferDialogComponent` (submit **disabled while the form is empty**,
enabled once a valid destination/amount/reference is entered) → send → the
failure toast surfaces for an invalid destination wallet **without any balance
drift**. The transfer form proxies through the new BFF endpoint
`POST /api/bff/transfers` (session-cookie auth).

The API-level saga scenarios (`tests/transfer.spec.ts`, fraud → hold → atomic
settle) ship with PR #262 and pass there; together both specs cover the full
UC-005 surface (UI + services).

Raw output: `results.json` (Playwright JSON reporter), `../html-report/` (HTML).
