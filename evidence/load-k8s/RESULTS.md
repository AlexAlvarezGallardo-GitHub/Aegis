# Aegis — Load Testing Evidence (Kubernetes, Phase 2)

> **Sandbox:** Minikube (docker driver, 4 vCPU / 8 GB) — Kubernetes v1.30.
> **Date:** 2026-08-08 — **Tool:** k6 (grafana/k6 container) → port-forward → BFF.
> **Topology:** ArgoCD-managed dev stack (aegis-dev), 1 replica per service,
> resource limits `500m CPU / 768Mi` (from `overlays/dev/*-values.yaml`).
> Local images (`infra-aegis-*`, incl. the BFF deposit fix) used with `pullPolicy=Never`.
> Raw outputs: `login.txt`, `wallets.txt`, `deposits.txt`, `idempotency.txt` + `*-summary.json`.

## Results

| Scenario | Requests | Throughput | Endpoint metric | p95 | SLO (300ms) | Errors |
|----------|----------|-----------|-----------------|-----|-------------|--------|
| `login` | 458 | 3.8 req/s | `aegis_login_latency` | **10.0s** | ❌ | 81% |
| `wallets` | 629 | 5.2 req/s | `aegis_create_wallet_latency` | 86ms | ✅ | — |
| `wallets` | — | — | `aegis_list_wallets_latency` | 33ms | ✅ | 44% (login step) |
| `deposits` | 443 | 3.7 req/s | `aegis_deposit_latency` | 25ms | ✅ | 3.6% |
| `idempotency` | 250 | 3.9 req/s | checks: 201-first / 409-repeat / never 5xx | — | ✅ | 0% (checks 100%) |

The wallet/deposit write+read paths **meet the 300 ms SLO in Kubernetes**. The
auth path collapses under load.

## Findings

### 1. Auth path fails under K8s resource limits (p95 ~10 s, 81% errors)
Login p95 is ~10,000 ms — a server-side timeout: the BFF's RestClient to the
Identity service times out at ~10 s. The Identity pod is limited to **500 m CPU**,
and each login runs **BCrypt** (~100 ms+ at rest, far more when CPU-throttled)
plus a refresh-token insert and a synchronous Kafka event. Under 50 VUs the
CPU is saturated, logins exceed 10 s, and the BFF returns 5xx. In docker-compose
(no CPU limit) the same scenario gave p95 ~840 ms.

Implications for the SLO (`docs/observability/slo.md`, p95 = 300 ms):
- The default dev resource limits are far too small for the auth path under load.
- Options: raise `identity` CPU limit / add replicas + HPA, lower BCrypt cost,
  or move the auth event publication off the request path (outbox).

### 2. Race condition in `AuthenticateUserService` (real bug)
Concurrent logins for the **same user** fail with
`org.hibernate.StaleObjectStateException: Row was updated or deleted by another
transaction` (User `@Version` optimistic lock). Controlled test: 40 concurrent
logins for one user → 18× 200, **22× 500**. The domain service calls
`userRepository.saveAndFlush(user)` on a `@Version`ed entity with no retry
(`AuthenticateUserService.authenticate`). Consequence: any retry / multi-replica /
same-user concurrency makes auth fail ~50%. This is a genuine correctness bug
and a likely cause of flaky auth-related tests.

## Phase 1 vs Phase 2 (compose vs K8s)

| Metric | docker-compose | Minikube (K8s) |
|--------|----------------|----------------|
| login p95 | 840 ms | ~10 s (timeout) |
| create wallet p95 | 34 ms | 86 ms |
| list wallets p95 | 18 ms | 33 ms |
| deposit p95 | 15 ms | 25 ms |
| infra constraints | none (host CPU) | 500 m / 768 Mi + probes |

The K8s run is the only one that exposes resource-limit and concurrency
behaviour — exactly what phase 2 was meant to validate.

## Reproduction (as actually executed)

1. `minikube start --driver=docker --cpus 4 --memory 8192`
2. Build + load local images: `docker compose build aegis-identity aegis-wallet aegis-bff`, then `minikube image load infra-aegis-{identity,wallet,bff}`
3. The cluster already had an ArgoCD-managed dev stack; to run local images:
   - pause ArgoCD: `kubectl scale sts argocd-application-controller -n argocd --replicas=0`
   - inject a shared strong `JWT_SECRET` (≥256 bits) into `{bff,identity,wallet}-dev-config`
   - `kubectl patch deployment {bff,identity,wallet}-dev` → image `infra-aegis-*:latest`, `imagePullPolicy: Never`
4. `kubectl port-forward --address 0.0.0.0 -n aegis-dev svc/{identity-dev,bff-dev,wallet-dev} 8081/8082/8083`
5. Seed pools + run scenarios (see `load/README.md`).
