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

    subgraph Cluster[Kubernetes Cluster]
        ArgoCD[Argo CD]
        DEV[DEV Namespace]
        PRE[PRE Namespace]
        STAGE[STAGE Namespace]
        PROD[PROD Namespace]
    end

    AppCode -->|push to main| CI
    CI -->|docker push| GHCR
    CI -->|update image tags| Overlays
    GHCR --> ArgoCD
    Charts --> Overlays
    Overlays --> ArgoApp
    ArgoCD -->|sync| DEV
    ArgoCD -->|sync| PRE
    ArgoCD -->|sync| STAGE
    ArgoCD -->|sync| PROD

    style AppCode fill:#bbf,color:#000
    style CI fill:#bbf,color:#000
    style GHCR fill:#fdb,color:#000
    style Charts fill:#bbf,color:#000
    style Overlays fill:#bbf,color:#000
    style ArgoApp fill:#bbf,color:#000
    style ArgoCD fill:#fdb,color:#000
    style DEV fill:#afa,color:#000
    style PRE fill:#afa,color:#000
    style STAGE fill:#afa,color:#000
    style PROD fill:#afa,color:#000
```

## Deployment Flow

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant CI as GitHub Actions CI
    participant GHCR as GitHub Container Registry
    participant GitOps as Aegis-GitOps
    participant Argo as Argo CD
    participant K8s as Kubernetes

    Dev->>CI: Push to main
    CI->>CI: Build & test
    CI->>GHCR: Push Docker images
    CI->>GitOps: Update image tags (overlays/dev)
    GitOps-->>Argo: Detect manifest change
    Argo->>Argo: Sync application
    Argo->>K8s: Apply manifests
    K8s-->>Argo: Deployment status
    Argo-->>GitOps: Sync result
```

## GitOps Repository Structure

```
Aegis-GitOps/
├── applications/          # Argo CD Application manifests
│   └── dev/               # One application per service
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
└── infrastructure/        # Platform infrastructure
    └── argocd/
```

## Environments

| Environment | Overlay | Namespace | Promotes to |
|-------------|---------|-----------|-------------|
| DEV   | `overlays/dev/`   | `aegis-dev`   | PRE |
| PRE   | `overlays/pre/`   | `aegis-pre`   | STAGE |
| STAGE | `overlays/stage/` | `aegis-stage` | PROD |
| PROD  | `overlays/prod/`  | `aegis-prod`  | - |

See [[05 - Infrastructure/Argo CD\|Argo CD]] for application reconciliation and [[05 - Infrastructure/Helm Charts\|Helm Charts]] for chart and overlay details.
