# Aegis — Load Testing Evidence (Kubernetes, Phase 2 — post-fix)

> **Sandbox:** Minikube (docker driver, 4 vCPU / 8 GB) — Kubernetes v1.30, namespace `aegis-dev`.
> **Date:** 2026-08-08 — **Tool:** k6 (grafana/k6 container) → port-forward → BFF.
> **Code fixes applied:** optimistic-lock race in login (pessimistic `PESSIMISTIC_WRITE` lock on `findByEmail`) + BCrypt cost 12 → 10 (new hashes only; pools re-seeded with `fx*` prefixes).
> **Environment tweak:** Identity `limits.cpu` 500m → 2 cores (the remaining auth bottleneck).
> Raw outputs: `*.txt` + `*-summary.json`.

## Results after fix

| Scenario | Requests | Endpoint metric | p95 | SLO (300ms) | Errors |
|----------|----------|-----------------|-----|-------------|--------|
| `login` | 2278 | `aegis_login_latency` | **1,085ms** | ❌ (improved 10x) | **0%** |
| `wallets` | 605 | create / list | 19 / 13ms | ✅ | 0% (endpoints) |
| `deposits` | 444 | `aegis_deposit_latency` | 16ms | ✅ | 3.2% (login step) |
| `idempotency` | 240 | checks: 201→409 | — | ✅ | checks 100% |

## Before / After — login scenario (50 VUs)

| Metric | k8s BEFORE (cost-12, 500m, race) | k8s AFTER (cost-10, 2 cores, lock fix) |
|--------|----------------------------------|----------------------------------------|
| Errors | 81.6% (BFF 10s timeout) | **0%** |
| p95 | 10,023ms (timeout ceiling) | **1,085ms** |
| avg | 6,650ms | 390ms |
| Throughput | 3.4 req/s | 18.9 req/s |
| Checks | 18% | 100% |

## What each change contributed

1. **Race condition fix** — `UserJpaRepository.findByEmail` now uses
   `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Verified: 40 concurrent logins for the
   same user went from 18×200/22×500 → **40×200/0×500**. (The pessimistic lock
   serializes same-user auth state changes instead of throwing
   `StaleObjectStateException`.)
2. **BCrypt cost 12 → 10** — halves per-login CPU cost (~120ms → ~60ms). Only
   affects newly registered users (cost is embedded in each stored hash), so
   pools were re-seeded.
3. **Identity CPU 500m → 2 cores** — the decisive lever. At 500m the identity can
   only serve ~8 logins/s (BCrypt is CPU-bound), so 50 VUs queue past the BFF's
   10s read timeout and fail. With 2 cores the same demand is absorbed.

## Remaining gap

Login p95 ~1.09s still exceeds the 300ms SLO. The floor is the BCrypt verify on
the request path plus K8s overhead. Options to close it: scale identity
replicas/HPA, move auth events fully off-path, or switch to Argon2id.

## Reproduction

Same as `evidence/load-k8s/RESULTS.md`, plus:
- `mvn -pl aegis-identity-service -am test` → BUILD SUCCESS (80 tests).
- Rebuild + load image with a **unique tag** (`infra-aegis-identity:authfix1`) —
  `minikube image load` does not reliably replace a same-tag image.
- `kubectl patch deployment identity-dev --type strategic` (JSON merge patch
  replaces the containers array and drops the image — must use strategic merge).
