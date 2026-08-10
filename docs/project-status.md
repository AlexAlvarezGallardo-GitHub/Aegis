# Aegis Project Status

> **Purpose:** In less than one minute, a visitor must be able to tell what actually works in Aegis. This matrix is the single source of truth for capability status — it must stay in sync with [`docs/architecture/service-catalog.md`](architecture/service-catalog.md) and `README.md`.

## Capability status matrix

| Capability | Código | Tests | CI | DEV | PRE | STAGE | PROD |
|------------|:------:|:-----:|:--:|:---:|:---:|:-----:|:----:|
| User registration (Identity) | ✅ | ✅ | ✅ | ✅ | ⚪ | ⚪ | ⚪ |
| User authentication (Identity + BFF) | ✅ | ✅ | ✅ | ✅ | ⚪ | ⚪ | ⚪ |
| BFF session handling (HttpOnly cookies, Redis) | ✅ | ✅ | ✅ | ✅ | ⚪ | ⚪ | ⚪ |
| Wallet creation (Wallet) | ✅ | ✅ | ✅ | ✅ | ⚪ | ⚪ | ⚪ |
| Wallet management (update, deactivate, reactivate) | ✅ | ✅ | ✅ | ✅ | ⚪ | ⚪ | ⚪ |
| Deposit funds (Wallet, transactional outbox) | ✅ | ✅ | ✅ | ✅ | ⚪ | ⚪ | ⚪ |
| Fraud evaluation (Fraud) | ✅ | ✅ | ✅ | ✅ | ⚪ | ⚪ | ⚪ |
| Audit events (Audit) | ✅ | ✅ | ✅ | ✅ | ⚪ | ⚪ | ⚪ |
| Reporting (Reporting) | 🟡 | 🟡 | ✅ | 🟡 | ⚪ | ⚪ | ⚪ |
| Payments (Payment) | 🟡 | 🟡 | ✅ | 🟡 | ⚪ | ⚪ | ⚪ |
| Notifications (Notification) | — | — | — | — | — | — | — |

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Available and validated |
| 🟡 | Partial — core works but not all capabilities are operational |
| ⚪ | Prepared — structure exists (GitOps manifests), not operating |
| — | Not available / not implemented |

> The **DEV** column refers to the verified docker-compose DEV stack (see [Environment status](#environment-status)). The GitOps/Kubernetes DEV structure has partial evidence (minikube, 2026-08-07) but is not yet an independently verified operating cluster.

## Environment status

| Environment | Status | Notes |
|-------------|--------|-------|
| **Local** | Functional | `docker-compose` dev stack: all 7 services, frontend, PostgreSQL ×6, Kafka, Redis |
| **DEV** | Functional (docker-compose) / Partial (minikube) | The docker-compose DEV stack is verified running. The Kubernetes + Argo CD structure exists in `Aegis-GitOps` (charts, `applications/dev/`, app-of-apps). Evidence from 2026-08-07 shows the wallet service deployed on minikube (`aegis-dev`) with a 40/40 deposit load test (p95 247 ms) and traces exported to Tempo — see `evidence/observability/`. Argo CD bootstrap/sync to `Aegis-GitOps` is still not independently confirmed — see `docs/HANDOVER.md` |
| **PRE** | Prepared structure | Helm values and overlays exist in `Aegis-GitOps`; no operating cluster |
| **STAGE** | Prepared structure | Helm values and overlays exist in `Aegis-GitOps`; no operating cluster |
| **PROD** | Prepared structure | Helm values and overlays exist in `Aegis-GitOps`; no operating cluster |

> PRE, STAGE and PROD demonstrate the intended promotion structure but are not currently operating production environments. There is no customer traffic, no regulatory certification, no real banking or KYC provider, and no commercial SLA.

> **Note:** `docs/HANDOVER.md` (2026-08-02) records that Argo CD was not yet connected to the GitOps repo, so earlier claims of "DEV runs in minikube via Argo CD" were unverified. Evidence added on 2026-08-07 (`evidence/observability/load-test-deposits.md`, Tempo trace exports, Grafana dashboards) shows the Kubernetes DEV stack running on minikube, so the environment is now marked **partial** rather than unverified. Argo CD bootstrap/sync should be confirmed with a health check before marking DEV **fully verified**.

## Evidence index

Every claim above points to verifiable evidence:

| Claim | Evidence |
|-------|----------|
| Code exists | `backend/` modules, `infra/docker-compose.yml` |
| Tests run in CI | `.github/workflows/ci.yml`, `.github/workflows/pr-validation.yml` |
| Security checks in CI | `.github/workflows/security.yml` (CodeQL, Trivy, SBOM, Scorecard, Cosign) |
| DEV is GitOps-managed | `Aegis-GitOps` repository: Helm charts, `applications/dev/`, app-of-apps |
| DEV Kubernetes stack running | `evidence/observability/load-test-deposits.md` (40/40 deposits, p95 247 ms, 2026-08-07), Tempo traces, Grafana dashboards |
| Observability | Prometheus, Grafana, Loki, Tempo (instrumented in `main`) |

## Update rules

1. Change a capability status → update **this matrix**, `docs/architecture/service-catalog.md`, and `README.md` in the same commit.
2. Environments must never be marked "active" without a running, verified cluster.
3. A capability is only ✅ in CI when there is a job that actually exercises it.
