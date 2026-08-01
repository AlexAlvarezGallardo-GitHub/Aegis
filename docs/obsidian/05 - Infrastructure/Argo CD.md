---
type: infrastructure
tags: [argocd, gitops, kubernetes, deployment, promotion]
status: implemented
---

# Argo CD

Argo CD is the GitOps engine of the Aegis platform. It is bootstrapped via GitOps, watches the `Aegis-GitOps` repository, and continuously reconciles the Kubernetes cluster with the declared state. No manual `kubectl apply` is used.

## Bootstrap

Argo CD installs itself from the upstream manifests and then self-manages its own configuration, RBAC and applications.

```mermaid
sequenceDiagram
    participant Ops as Operator
    participant K8s as Kubernetes
    participant Argo as Argo CD
    participant GitOps as Aegis-GitOps

    Ops->>K8s: kubectl apply -k infrastructure/argocd/install
    K8s->>Argo: Deploy Argo CD (upstream manifests)
    Ops->>Argo: kubectl -n argocd apply -f applications/aegis-app-of-apps.yaml
    Argo->>GitOps: Poll applications/
    GitOps-->>Argo: Child applications
    Argo->>Argo: Self-manage config, RBAC, apps
```

## App of Apps

The root `aegis-app-of-apps` manages every child application, including itself.

```mermaid
graph TB
    Root[aegis-app-of-apps] --> Dev[DEV Applications]
    Root --> Pre[PRE Applications]
    Root --> Stage[STAGE Applications]
    Root --> Prod[PROD Applications]
    Root --> Mon[Monitoring]
    Root --> Log[Logging]

    Dev --> DevS[identity, wallet, bff, frontend]
    Pre --> PreS[identity, wallet, bff, frontend]
    Stage --> StageS[identity, wallet, bff, frontend]
    Prod --> ProdS[identity, wallet, bff, frontend]

    style Root fill:#fdb,color:#000
    style Dev fill:#bbf,color:#000
    style Pre fill:#bbf,color:#000
    style Stage fill:#bbf,color:#000
    style Prod fill:#bbf,color:#000
    style Mon fill:#bbf,color:#000
    style Log fill:#bbf,color:#000
```

## Sync Flow

```mermaid
sequenceDiagram
    participant Repo as Aegis-GitOps
    participant Argo as Argo CD
    participant K8s as Kubernetes Cluster
    participant Svc as Service (Helm chart)

    Argo->>Repo: Poll for changes
    Repo-->>Argo: Manifests / values
    Argo->>Argo: Render Helm chart
    Argo->>K8s: Apply manifests (sync)
    K8s-->>Argo: Status
    Argo->>Argo: Compare live vs desired
    alt Drift detected
        Argo->>K8s: Self-heal (apply desired)
    end
```

## Applications

Each service is deployed per environment. The application points at its Helm chart with the overlay values for that environment:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: identity-pre
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/AlexAlvarezGallardo-GitHub/Aegis-GitOps
    targetRevision: main
    path: charts/identity
    helm:
      valueFiles:
        - ../../overlays/pre/identity-values.yaml
  destination:
    server: https://kubernetes.default.svc
    namespace: aegis-pre
```

## Sync Policies

| Environment | Sync | Prune | Self-Heal |
|-------------|------|-------|-----------|
| DEV   | Auto  | Yes | Yes |
| PRE   | Auto  | Yes | Yes |
| STAGE | Manual/Approval | Yes | Yes |
| PROD  | Manual/Approval | No  | Yes |

## Environment Promotion

Promotion is declarative: updating the image tag in `overlays/<env>/*-values.yaml` is the promotion. DEV and PRE sync automatically; STAGE and PROD require a manual sync (approval gate).

```mermaid
graph LR
    A[DEV] -->|auto| B[PRE]
    B -->|approval gate| C[STAGE]
    C -->|approval gate| D[PROD]
    style A fill:#afa,color:#000
    style B fill:#afa,color:#000
    style C fill:#afa,color:#000
    style D fill:#afa,color:#000
```

## Health Checks

Every chart configures liveness and readiness probes (`/actuator/health` for backends, `/` for frontend). Argo CD uses these for application health assessment.

Related: [[05 - Infrastructure/GitOps\|GitOps]], [[05 - Infrastructure/Helm Charts\|Helm Charts]].
