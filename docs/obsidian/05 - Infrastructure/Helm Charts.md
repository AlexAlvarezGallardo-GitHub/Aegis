---
type: infrastructure
tags: [helm, kubernetes, overlays, charts]
status: implemented
---

# Helm Charts

Each Aegis service is packaged as a standalone Helm chart under `charts/<service>/` in `Aegis-GitOps`. Environment differences are handled through Kustomize overlays that provide per-environment Helm value files.

## Chart Structure

```mermaid
graph TB
    Chart[charts/<service>] --> ChartYaml[Chart.yaml]
    Chart --> Values[values.yaml]
    Chart --> Templates[templates/]

    Templates --> Deployment[deployment.yaml]
    Templates --> Service[service.yaml]
    Templates --> ConfigMap[configmap.yaml]
    Templates --> Ingress[ingress.yaml]
    Templates --> HPA[hpa.yaml]
    Templates --> PDB[pdb.yaml]

    style Chart fill:#fdb,color:#000
    style ChartYaml fill:#afa,color:#000
    style Values fill:#afa,color:#000
    style Templates fill:#afa,color:#000
    style Deployment fill:#bbf,color:#000
    style Service fill:#bbf,color:#000
    style ConfigMap fill:#bbf,color:#000
    style Ingress fill:#bbf,color:#000
    style HPA fill:#bbf,color:#000
    style PDB fill:#bbf,color:#000
```

## Overlay Strategy

Environment-specific configuration is provided via Helm value files in `overlays/<env>/`. Argo CD applications point a chart at `overlays/<env>/<service>-values.yaml`.

```mermaid
graph LR
    Base[base/values.yaml] --> Dev[overlays/dev/*-values.yaml]
    Base --> Pre[overlays/pre/*-values.yaml]
    Base --> Stage[overlays/stage/*-values.yaml]
    Base --> Prod[overlays/prod/*-values.yaml]

    style Base fill:#fdb,color:#000
    style Dev fill:#bbf,color:#000
    style Pre fill:#bbf,color:#000
    style Stage fill:#bbf,color:#000
    style Prod fill:#bbf,color:#000
```

## Environment Parameters

| Parameter | DEV | PRE | STAGE | PROD |
|-----------|-----|-----|-------|------|
| Replicas | 1 | 2 | 2 | 3 (HPA 3-10) |
| CPU request | 100m | 250m | 250m | 500m |
| Memory request | 128Mi | 256Mi | 256Mi | 512Mi |
| HPA | No | No | No | Yes |
| PDB | No | No | Yes (1) | Yes (2) |
| Affinity | No | No | No | Yes (anti-affinity) |

## Templates

Each chart renders these Kubernetes resources:

- **Deployment**: replicas, image, probes, resources, affinity, tolerations. Replica count is omitted when HPA is enabled.
- **Service**: ClusterIP service on the service port.
- **ConfigMap**: environment variables from `.Values.config`.
- **Ingress**: optional, enabled via `.Values.ingress.enabled`.
- **HorizontalPodAutoscaler**: optional, enabled via `.Values.autoscaling.enabled`.
- **PodDisruptionBudget**: optional, enabled via `.Values.podDisruptionBudget.enabled`.

## Services

| Service | Chart | Image | Port | Probe |
|---------|-------|-------|------|-------|
| [[01 - Services/Identity Service\|Identity Service]] | `charts/identity` | `identity-service` | 8081 | `/actuator/health` |
| [[01 - Services/Wallet Service\|Wallet Service]] | `charts/wallet` | `wallet-service` | 8083 | `/actuator/health` |
| [[01 - Services/BFF Service\|BFF Service]] | `charts/bff` | `bff-service` | 8082 | `/actuator/health` |
| [[01 - Services/Frontend\|Frontend]] | `charts/frontend` | `frontend` | 80 | `/` |

## Validation

- `helm lint` passes for all charts (0 failures)
- `helm template` verified for default and production overlays
- `kubectl kustomize` validates all overlays

Related: [[05 - Infrastructure/GitOps\|GitOps]], [[05 - Infrastructure/Argo CD\|Argo CD]].
