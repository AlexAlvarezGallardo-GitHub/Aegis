# Load Testing (k6)

> **Status:** Run and evidenced against the local docker-compose sandbox.
> Results: `../evidence/load/RESULTS.md` (+ raw `*.txt` / `*-summary.json`).

## Tooling

[k6](https://k6.io) via the `grafana/k6` Docker image — no local install needed.
Scripts live in `load/k6/`.

## Scenarios

| Script | Scenario | Key assertion | SLO gate |
|--------|----------|---------------|----------|
| `login.js` | BFF login flow | 200 + `SESSION` cookie; p95 < 1200 ms | auth p95 |
| `wallets.js` | Create (once/VU) + list wallets | 201 / 200 | list & create p95 < 300 ms |
| `deposits.js` | Concurrent deposits (UC-004) | 201 + `newBalance`; p95 < 400 ms | deposit p95 |
| `idempotency.js` | Reuse the same deposit reference | first 201, repeats 409, never 5xx | checks == 1 |

The BFF contract is session-cookie based: `SESSION` cookie + rotating `XSRF-TOKEN`
(cookie CSRF) header on state-changing calls. The BFF also rotates the `SESSION`
cookie (session-fixation protection) on every write, so each iteration
authenticates fresh (`lib.js`).

## One-off run

```powershell
# Start the stack (BFF on :8082)
docker compose -f infra/docker-compose.yml up -d

# Seed a pool of users, then run a scenario with that pool:
.\load\seed-users.ps1 -Prefix depuser -Count 20
docker run --rm `
  -v "${PWD}\load:/scripts" `
  -v "${PWD}\evidence\load:/out" `
  grafana/k6:latest run `
  --summary-export /out/deposits-summary.json `
  /scripts/k6/deposits.js `
  -e BASE_URL=http://host.docker.internal:8082 `
  -e USER_PREFIX=depuser
```

`BASE_URL` defaults to the BFF (`http://localhost:8082`). Inside the k6
container use `http://host.docker.internal:8082` (Docker Desktop).

## Full evidence run

```powershell
.\load\seed-users.ps1 -Prefix lguser  -Count 50
.\load\seed-users.ps1 -Prefix wltuser -Count 30
.\load\seed-users.ps1 -Prefix depuser -Count 20
.\load\seed-users.ps1 -Prefix idemuser -Count 10
.\load\run-load-tests.ps1
```

Writes `evidence/load/<scenario>.txt` and `<scenario>-summary.json`.

## Notes / gotchas

- **Fresh pool per scenario** — the domain enforces max 5 wallets per user
  (`WALLET_LIMIT_EXCEEDED`), so `seed-users.ps1` gives every scenario its own
  users.
- **CSRF**: the BFF rotates the `XSRF-TOKEN` cookie on every state-changing
  request; `lib.js` re-reads it from the cookie jar before each call.
- **Session reuse is not reliable with k6's jar** (SESSION rotates on writes),
  hence login-per-iteration in the scenarios.
- **Idempotency** intentionally returns 409 for repeated references — k6 counts
  it as `http_req_failed`, so that scenario gates on `checks == 1` instead.

## Known findings

See `../evidence/load/RESULTS.md`:
- Login p95 (~840 ms at 50 VUs) exceeds the 300 ms SLO — auth is the bottleneck
  (BCrypt + refresh-token persist + sync Kafka event in identity).
- Wallet endpoints are fast (create 34 ms / list 18 ms / deposit 15 ms p95).

## CI integration (planned)

A `load-test.yml` workflow can run `run-load-tests.ps1` against the DEV sandbox
on schedule and publish the evidence as an artifact.

## Phase 2 — Kubernetes sandbox (Minikube)

Results and findings: `../evidence/load-k8s/RESULTS.md`.

The Kubernetes run (same k6 scenarios, 1 replica per service, `500m CPU / 768Mi`
limits) shows the wallet/deposit endpoints still meet the 300 ms SLO, but the
auth path collapses: **login p95 ~10 s with ~81% errors** at 50 VUs (BFF →
Identity timeout; Identity is CPU-bound on BCrypt at 500 m). It also exposed a
real bug: concurrent logins for the same user throw
`StaleObjectStateException` (~50% 500s) in `AuthenticateUserService`.

`scripts/load-sandbox-minikube.ps1` scaffolds the K8s sandbox (Minikube + GitOps
charts). Known caveats discovered when running it against a cluster that already
had an ArgoCD-managed stack:
- **ArgoCD selfHeal reverts manual patches.** To run local images, scale the
  controller down first: `kubectl scale sts argocd-application-controller -n argocd --replicas=0`.
- A shared strong `JWT_SECRET` (≥256 bits) must be injected into the three
  `*-dev-config` ConfigMaps, otherwise the services fail with
  `io.jsonwebtoken.security.WeakKeyException` (the committed remote values do not
  carry a JWT secret).
- The BFF references `identity-dev` / `wallet-dev` by DNS, so Helm releases must
  be named `identity-dev` / `wallet-dev` / `bff-dev`.
- The GitOps repo's `overlays/dev/*-values.yaml` moved DB credentials to a
  Secret (`aegis-dev-credentials`) that `scripts/setup-minikube.ps1` creates;
  this is uncommitted WIP and must be reconciled before trusting ArgoCD syncs.

## See also

- [SLIs and SLOs](../docs/observability/slo.md)
- [Deposit Flow](../docs/architecture/sequences/deposit-flow.md)
