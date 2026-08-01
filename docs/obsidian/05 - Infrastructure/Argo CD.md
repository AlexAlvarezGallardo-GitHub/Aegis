---
type: infrastructure
tags: [argocd, gitops, kubernetes, deployment]
status: implemented
---

# Argo CD

Argo CD is the GitOps engine of the Aegis platform. It watches the `Aegis-GitOps` repository and continuously syncs the declared manifests into the Kubernetes cluster, keeping each environment in the exact state described in Git.

## App of Apps

```mermaid
graph TB
    AppOfApps[App of Apps] --> Identity[Identity Application]
    AppOfApps --> Wallet[Wallet Application]
    AppOfApps --> BFF[BFF Application]
    AppOfApps --> Frontend[Frontend Application]
    AppOfApps --> Monitoring[Monitoring Application]

    Identity --> ChartsI[charts/identity]
    Wallet --> ChartsW[charts/wallet]
    BFF --> ChartsB[charts/bff]
    Frontend --> ChartsF[charts/frontend]
    Monitoring --> Infra[infrastructure/]

    style AppOfApps fill:#fdb,color:#000
    style Identity fill:#bbf,color:#000
    style Wallet fill:#bbf,color:#000
    style BFF fill:#bbf,color:#000
    style Frontend fill:#bbf,color:#000
    style Monitoring fill:#bbf,color:#000
    style ChartsI fill:#afa,color:#000
    style ChartsW fill:#afa,color:#000
    style ChartsB fill:#afa,color:#000
    style ChartsF fill:#afa,color:#000
    style Infra fill:#afa,color:#000
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

Each service is deployed as an independent Argo CD Application targeting its Helm chart with the environment overlay values:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: identity-dev
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/AlexAlvarezGallardo-GitHub/Aegis-GitOps
    targetRevision: main
    path: charts/identity
    helm:
      valueFiles:
        - ../../overlays/dev/identity-values.yaml
  destination:
    server: https://kubernetes.default.svc
    namespace: aegis-dev
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

## Environment Promotion

Promotion is declarative: to promote a service to a higher environment, the image tag in the corresponding overlay values file is updated in Git. Argo CD picks the change up and syncs automatically.

```mermaid
graph LR
    A[DEV overlay values] -->|promote tag| B[PRE overlay values]
    B -->|promote tag| C[STAGE overlay values]
    C -->|promote tag| D[PROD overlay values]
    style A fill:#bbf,color:#000
    style B fill:#bbf,color:#000
    style C fill:#bbf,color:#000
    style D fill:#bbf,color:#000
```

Related: [[05 - Infrastructure/GitOps\|GitOps]], [[05 - Infrastructure/Helm Charts\|Helm Charts]].
