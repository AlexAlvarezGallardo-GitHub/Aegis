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
        PRE[PRE Namespace - future]
        STAGE[STAGE Namespace - future]
        PROD[PROD Namespace - future]
    end

    AppCode -->|push to main| CI
    CI -->|docker push| GHCR
    CI -->|update image tags| Overlays
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
    CI->>GHCR: Push Docker images
    CI->>GitOps: Update image tags (overlays/dev/*-values.yaml)
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
│   ├── dev/                   # One application per dev service/infra
│   │   ├── bff.yaml
│   │   ├── database.yaml
│   │   ├── frontend.yaml
│   │   ├── identity.yaml
│   │   ├── kafka.yaml
│   │   ├── redis.yaml
│   │   └── wallet.yaml
│   ├── pre/               # (future app-of-apps per environment)
│   ├── stage/
│   └── prod/
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
    ├── argocd/
    ├── database/          # postgres-identity, postgres-wallet
    ├── kafka/             # kafka, zookeeper
    └── redis/             # redis
```

## Environments

| Environment | Overlay | Namespace | Status |
|-------------|---------|-----------|--------|
| DEV   | `overlays/dev/`   | `aegis-dev`   | **Active** (minikube) |
| PRE   | `overlays/pre/`   | `aegis-pre`   | Not deployed |
| STAGE | `overlays/stage/` | `aegis-stage` | Not deployed |
| PROD  | `overlays/prod/`  | `aegis-prod`  | Not deployed |

Each environment gets its own app-of-apps Application (`app-of-apps-<env>` pointing at `applications/<env>/`) when it becomes active. Overlays and charts for pre/stage/prod already exist in the repo and are promotion-ready.

See [[05 - Infrastructure/Argo CD\|Argo CD]] for application reconciliation and [[05 - Infrastructure/Helm Charts\|Helm Charts]] for chart and overlay details.
