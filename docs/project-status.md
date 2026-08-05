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
| Payments (Payment) | — | — | — | — | — | — | — |
| Notifications (Notification) | — | — | — | — | — | — | — |

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Available and validated |
| 🟡 | Partial — core works but not all capabilities are operational |
| ⚪ | Prepared — structure exists (GitOps manifests), not operating |
| — | Not available / not implemented |

> The **DEV** column refers to the verified docker-compose DEV stack (see [Environment status](#environment-status)). The GitOps/Kubernetes DEV structure is not yet an operating cluster.

## Environment status

| Environment | Status | Notes |
|-------------|--------|-------|
| **Local** | Functional | `docker-compose` dev stack: all 6 services, frontend, PostgreSQL ×5, Kafka, Redis |
| **DEV** | Functional (docker-compose) / Prepared (GitOps) | The docker-compose DEV stack is verified running. The Kubernetes + Argo CD DEV structure exists in `Aegis-GitOps` (charts, `applications/dev/`, app-of-apps) but is **not verified** as an operating cluster — see `docs/HANDOVER.md` |
| **PRE** | Prepared structure | Helm values and overlays exist in `Aegis-GitOps`; no operating cluster |
| **STAGE** | Prepared structure | Helm values and overlays exist in `Aegis-GitOps`; no operating cluster |
| **PROD** | Prepared structure | Helm values and overlays exist in `Aegis-GitOps`; no operating cluster |

> PRE, STAGE and PROD demonstrate the intended promotion structure but are not currently operating production environments. There is no customer traffic, no regulatory certification, no real banking or KYC provider, and no commercial SLA.

> **Note:** A previous version of `docs/obsidian/00 - Overview/Aegis Platform.md` stated that DEV runs in minikube via Argo CD. `docs/HANDOVER.md` (2026-08-02) records that Argo CD was never connected to the GitOps repo, so that claim is **unverified** and must not be repeated until a health check confirms the cluster is actually operating.

## Evidence index

Every claim above points to verifiable evidence:

| Claim | Evidence |
|-------|----------|
| Code exists | `backend/` modules, `infra/docker-compose.yml` |
| Tests run in CI | `.github/workflows/ci.yml`, `.github/workflows/pr-validation.yml` |
| Security checks in CI | `.github/workflows/security.yml` (CodeQL, Trivy, SBOM, Scorecard, Cosign) |
| DEV is GitOps-managed | `Aegis-GitOps` repository: Helm charts, `applications/dev/`, app-of-apps |
| Observability | Prometheus, Grafana, Loki, Tempo (instrumented in `main`) |

## Update rules

1. Change a capability status → update **this matrix**, `docs/architecture/service-catalog.md`, and `README.md` in the same commit.
2. Environments must never be marked "active" without a running, verified cluster.
3. A capability is only ✅ in CI when there is a job that actually exercises it.
