# Aegis — Load Testing Evidence (k6)

> **Sandbox:** docker-compose local stack (`infra/docker-compose.yml`).
> **Date:** 2026-08-08 — **Tool:** k6 (grafana/k6 container) — **BFF:** `http://localhost:8082`
> **Stack:** JDK 21 (Spring Boot 3.3.5), PostgreSQL 16, Kafka 7.5, Redis 7 — single host laptop.

Each scenario uses its own fresh user pool (max 5 wallets/user domain rule).
Raw outputs: `login.txt`, `wallets.txt`, `deposits.txt`, `idempotency.txt`,
`transfers.txt` + `*-summary.json` (k6 summary-export).

## Results

| Scenario | Requests | Throughput | Endpoint metric | p50 | p95 | SLO (300ms) | Errors |
|----------|----------|-----------|-----------------|-----|-----|-------------|--------|
| `login` | 2216 | 18.3 req/s | `aegis_login_latency` | 337ms | **840ms** | ❌ breached | 0% |
| `wallets` | 2762 | 22.8 req/s | `aegis_list_wallets_latency` | 6.7ms | 18ms | ✅ | 0% |
| `wallets` | — | — | `aegis_create_wallet_latency` | 14ms | 34ms | ✅ | 0% |
| `deposits` | 1768 | 14.6 req/s | `aegis_deposit_latency` | 10.9ms | 15ms | ✅ | 0% |
| `idempotency` | 934 | 15.4 req/s | checks: 201-first / 409-repeat | — | — | ✅ | 0 unexpected (452 expected 409s) |
| `transfers` | 3144 | 26.2 req/s | `aegis_transfer_latency` (saga: fraud→hold→settle) | 32ms | **71ms** | ✅ | 0.31% (login warm-up) |
| `payments` | 3212 | 26.8 req/s | `aegis_payment_latency` (saga: fraud→hold→debit) | 36ms | **60ms** | ✅ | 0.45% (login warm-up) |

All thresholds configured in the scenarios passed; 100% of checks passed on every
run. The only metric above the documented SLO (`docs/observability/slo.md`,
p95 = 300 ms) is the authentication path. The UC-005 transfer saga completes in
~71 ms p95 and the UC-006 payment saga in ~60 ms p95 (fraud assessment + hold +
atomic settlement/debit + events), comfortably inside the 1500 ms budget set in
`load/k6/transfers.js` and `load/k6/payments.js`.

## Findings

### 1. Fix applied — BFF deposit contract mismatch (bug)
`POST /api/bff/wallets/{id}/deposits` sent `{amount, method, reference}` to the
Wallet Service, which requires `{amount, currency, source, reference}` (all
`@NotBlank`). Every deposit through the BFF returned **500**. The Angular app
already sent the correct payload, so the deposit feature was broken end-to-end
via the BFF. Aligned the BFF DTO/port/client with the Wallet Service contract and
the OpenAPI specs (`specs/010-bff`, `specs/004-deposit-funds`).

### 2. Auth path does not meet the latency SLO (perf finding)
Single login ≈ 270–430 ms; p95 climbs to ~840 ms at 50 VUs. The Identity login
performs BCrypt verification + refresh-token persistence + a synchronous
`UserAuthenticated` Kafka event. Even at rest it exceeds the 300 ms p95 SLO.
Recommended: publish the auth event async/outbox, tune BCrypt cost, or increase
identity replicas in the k8s sandbox (phase 2).

### 3. Session/CSRF mechanics matter for load-test design
The BFF rotates the `SESSION` cookie (session-fixation protection) and the
`XSRF-TOKEN` cookie on every state-changing request. Reusing a session across
iterations breaks with k6's cookie jar → each scenario logs in per iteration
(fresh authenticated context). This mirrors a realistic client and keeps the
wallet/deposit metrics independent of auth.

### 4. Domain rule: max 5 wallets per user
`WALLET_LIMIT_EXCEEDED` (409) kicks in at 5 wallets/user. Load scripts create a
wallet **once per VU** and each scenario must seed its own fresh user pool
(`load/seed-users.ps1`). The transfers scenario reuses existing EUR wallets and
tops them up each iteration so a warm pool never runs dry.

### 5. UC-005 transfer saga is fast and idempotent (2026-08-11)
First load evidence for the peer-to-peer transfer path (`load/k6/transfers.js`,
direct service API since the BFF transfer endpoint ships with the UI in #253):
p95 **71 ms** for the full saga (fraud assess → hold → atomic settle → events)
at up to 15 VUs, 100% check success. Duplicate-reference transfers are rejected
with 409 and never double-debit.

### 6. E2E/sandbox run surfaced and fixed four blocking bugs (2026-08-11)Booting a clean sandbox to run the UC-005 E2E (`e2e/tests/transfer.spec.ts`) found
real defects that unit tests could not catch:

1. **Reserved SQL keyword `offset`** in `processed_events` migrations of the
   payment, fraud and reporting services broke Flyway (syntax error at boot).
   Escaped as `"offset"` in the DDL and the `insertIfAbsent` native queries.
2. **`KafkaTemplate<String, String>` mismatch** in the payment and fraud
   `OutboxRelayScheduler` — no qualifying bean (the app defines
   `KafkaTemplate<String, Object>`), so both services failed to start. Switched
   the schedulers to `KafkaTemplate<String, Object>`.
3. **The saga orchestrator was incomplete**: the transfer stopped at
   `FRAUD_CHECK` because nothing called the wallet hold/settle steps. Implemented
   `RestWalletGateway` and wired fraud → hold → settle → `COMPLETED` with
   compensation (hold release) in `TransferService`.
4. **Payment → wallet service URL** defaulted to `localhost:8083`, which inside
   the container is not the wallet service. Set `AEGIS_PAYMENT_WALLET_BASE_URL`
   (and `AEGIS_PAYMENT_FRAUD_BASE_URL`) to the compose service names.

## Reproduction

```powershell
# 1. Start the stack
docker compose -f infra/docker-compose.yml up -d

# 2. Seed one fresh pool per scenario
.\load\seed-users.ps1 -Prefix lguser -Count 50
.\load\seed-users.ps1 -Prefix wltuser -Count 30
.\load\seed-users.ps1 -Prefix depuser -Count 20
.\load\seed-users.ps1 -Prefix idemuser -Count 10
.\load\seed-users.ps1 -Prefix trfuser -Count 15
.\load\seed-users.ps1 -Prefix payuser -Count 15

# 3. Run all scenarios + write evidence
.\load\run-load-tests.ps1
```

### 7. UC-006 payment saga is fast and secure (2026-08-12)
First load evidence for merchant payments (`load/k6/payments.js`): p95 **60 ms**
for the full saga at up to 15 VUs, 100% check success. The E2E run surfaced a
**contract/security bug** that unit tests missed: the payment controller accepted
`userId` from the request body (spoofable) and required it — a client could pay
from another user's wallet. Fixed to derive the user from the `X-User-Id` header
(BFF-populated from the session JWT), aligning the API with the OpenAPI contract
(`specs/006-execute-payment/contracts/api/payments-api.yaml`), which never had
`userId` in the body.
