---
type: infrastructure
tags: [gitops, argocd, helm, kubernetes, devops]
status: implemented
---

# GitOps

The Aegis platform is deployed through a GitOps workflow: the desired state of every environment is declared in the `Aegis-GitOps` repository, and Argo CD continuously reconciles the Kubernetes cluster with that state. Aegis is a single platform distributed across several repositories, and GitOps separates application code from deployment configuration.

## Repositories

| Repository | Purpose |
|------------|---------|
| `Aegis` | Application code (backend services, frontend, CI/CD workflows) |
| `Aegis-GitOps` | Declarative deployment configuration (Helm charts, overlays, Argo CD applications) |

## Architecture

```mermaid
graph TB
    subgraph AppRepo[Code Repository: Aegis]
        AppCode[Application Code]
        CI[GitHub Actions CI]
    end

    subgraph Registry[Container Registry]
        GHCR[(GitHub Container Registry)]
    end

    subgraph GitOpsRepo[GitOps Repository: Aegis-GitOps]
        Charts[Helm Charts]
        Overlays[Kustomize Overlays]
        ArgoApp[Argo CD Applications]
    end

    subgraph Cluster[Kubernetes Cluster - minikube]
        ArgoCD[Argo CD]
        AppOfApps[app-of-apps-dev]
        DEV[DEV Namespace: aegis-dev]
        PRE[PRE Namespace: aegis-pre]
        STAGE[STAGE Namespace: aegis-stage]
        PROD[PROD Namespace: aegis-prod]
    end

    AppCode -->|push to main| CI
    CI -->|docker push| GHCR
    CI -->|gitops-update PR| GitOpsRepo
    GHCR --> ArgoCD
    Charts --> Overlays
    Overlays --> ArgoApp
    ArgoCD -->|bootstrap + sync| AppOfApps
    AppOfApps -->|auto-discover| DEV
    ArgoCD -->|sync| DEV

    style AppCode fill:#bbf,color:#000
    style CI fill:#bbf,color:#000
    style GHCR fill:#fdb,color:#000
    style Charts fill:#bbf,color:#000
    style Overlays fill:#bbf,color:#000
    style ArgoApp fill:#bbf,color:#000
    style ArgoCD fill:#fdb,color:#000
    style AppOfApps fill:#fdb,color:#000
    style DEV fill:#afa,color:#000
    style PRE fill:#ddd,color:#000
    style STAGE fill:#ddd,color:#000
    style PROD fill:#ddd,color:#000
```

## Deployment Flow

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant CI as GitHub Actions CI
    participant GHCR as GitHub Container Registry
    participant GitOps as Aegis-GitOps
    participant Argo as Argo CD
    participant AppOfApps as app-of-apps-dev
    participant K8s as Kubernetes

    Dev->>CI: Push to main
    CI->>CI: Build & test
    CI->>GHCR: Push Docker images (immutable SHA tags)
    CI->>GitOps: gitops-update job opens PR (overlays/dev/*-values.yaml)
    GitOps-->>Argo: Detect manifest change
    Argo->>AppOfApps: Reconcile applications/
    AppOfApps->>K8s: Auto-discover child apps & sync
    K8s-->>Argo: Deployment status
    Argo-->>GitOps: Sync result
```

## Auto-Deployment via App-of-Apps

The root `app-of-apps-dev` Application points at `applications/dev/` (recurse: true). Any new Application manifest added to that directory is **auto-discovered and deployed** by Argo CD — no manual `kubectl apply` is needed. This is the single bootstrap point: it was applied once, and every subsequent Application flows through Git.

```mermaid
graph TB
    AppOfApps[app-of-apps-dev] --> BFF[bff-dev]
    AppOfApps --> DB[database]
    AppOfApps --> FE[frontend-dev]
    AppOfApps --> ID[identity-dev]
    AppOfApps --> KAFKA[kafka]
    AppOfApps --> REDIS[redis]
    AppOfApps --> WAL[wallet-dev]

    style AppOfApps fill:#fdb,color:#000
    style BFF fill:#bbf,color:#000
    style DB fill:#bbf,color:#000
    style FE fill:#bbf,color:#000
    style ID fill:#bbf,color:#000
    style KAFKA fill:#bbf,color:#000
    style REDIS fill:#bbf,color:#000
    style WAL fill:#bbf,color:#000
```

## GitOps Repository Structure

```
Aegis-GitOps/
├── applications/          # Argo CD Application manifests
│   ├── app-of-apps-dev.yaml   # Root app-of-apps (auto-discovers dev apps)
│   ├── monitoring.yaml        # Observability stack (ns monitoring)
│   ├── logging.yaml           # Loki + Promtail (ns logging)
│   ├── dev/                   # One application per dev service/infra
│   │   ├── bff.yaml
│   │   ├── database.yaml
│   │   ├── frontend.yaml
│   │   ├── identity.yaml
│   │   ├── kafka.yaml
│   │   ├── redis.yaml
│   │   └── wallet.yaml
│   ├── pre/               # bff, frontend, identity, wallet (no app-of-apps yet)
│   ├── stage/             # bff, frontend, identity, wallet
│   └── prod/              # bff, frontend, identity, wallet
├── base/                  # Shared reference values
├── charts/                # Helm charts per service
│   ├── identity/
│   ├── wallet/
│   ├── bff/
│   └── frontend/
├── overlays/              # Environment-specific Helm values
│   ├── dev/
│   ├── pre/
│   ├── stage/
│   └── prod/
└── infrastructure/        # Platform infrastructure (base/overlays pattern)
    ├── argocd/            # Argo CD install + config + RBAC
    ├── database/          # postgres-identity, postgres-wallet
    ├── kafka/             # kafka, zookeeper
    ├── redis/             # redis
    ├── monitoring/        # Prometheus, Grafana, Tempo, Alertmanager, exporters
    └── logging/           # Loki, Promtail
```

## Environments

| Environment | Overlay | Namespace | Sync | Prune | Self-Heal |
|-------------|---------|-----------|------|-------|-----------|
| DEV   | `overlays/dev/`   | `aegis-dev`   | Auto | Yes | Yes |
| PRE   | `overlays/pre/`   | `aegis-pre`   | Auto | Yes | Yes |
| STAGE | `overlays/stage/` | `aegis-stage` | Manual/Approval | Yes | Yes |
| PROD  | `overlays/prod/`  | `aegis-prod`  | Manual/Approval | **No** | Yes |

The `aegis-pre`, `aegis-stage` and `aegis-prod` applications already exist in `applications/` for the four service charts and are promotion-ready; only DEV has a root `app-of-apps` today (`app-of-apps-dev.yaml`).

## Deployable via GitOps

Argo CD deploys **only** the services that ship a Helm chart:

- `identity`, `wallet`, `bff`, `frontend` (service charts)
- `database`, `kafka`, `redis` (infrastructure)
- `monitoring`, `logging` (observability)

> **Note:** `audit`, `fraud` and `reporting` have **no Helm chart and no Argo CD Application**. They run only via docker-compose locally and ship images to GHCR through CI — they are **not** deployable via GitOps today.

## Images & Pull Secret

- Images are pushed to GHCR under `ghcr.io/alexalvarezgallardo-github/`:
  `identity-service`, `wallet-service`, `bff-service`, `frontend`
- Tags are immutable SHA tags (`tag: <git-sha>`), overridden per environment in `overlays/<env>/*-values.yaml`
- Pods pull images via the `ghcr-pull` imagePullSecret (created by `scripts/setup-minikube.ps1`)

## Bootstrap

- Argo CD installs itself via `infrastructure/argocd/install` (kustomize)
- `applications/app-of-apps-dev.yaml` is the single bootstrap point
- `scripts/setup-minikube.ps1` automates cluster creation, Argo CD install, GHCR pull secret and app-of-apps apply

## Promotion

The `gitops-update` CI job (`ci.yml`) opens a PR against `Aegis-GitOps` updating the dev image tags in `overlays/dev/*-values.yaml`. Promotion to pre/stage/prod follows the same manual-approval flow by bumping the respective overlay values.

See [[05 - Infrastructure/Argo CD\|Argo CD]] for application reconciliation and [[05 - Infrastructure/Helm Charts\|Helm Charts]] for chart and overlay details.
