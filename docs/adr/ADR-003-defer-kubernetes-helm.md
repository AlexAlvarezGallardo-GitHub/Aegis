# ADR-003: Defer Kubernetes and Helm to a Later Phase

## Status

Accepted

## Date

2026-07-31

## Context

The Aegis constitution (`.specify/memory/constitution.md`) mandates Kubernetes + Helm for production orchestration. However, the platform is currently in active development with the following services implemented:

- Identity Service (built & tested)
- BFF Service (built & tested)
- Wallet Service (built & tested)
- Common Module (shared library)
- Fraud Service (scaffolded)
- Reporting, Audit, Notification Services (planned)

The current development environment uses Docker Compose for local development and testing. Introducing Kubernetes and Helm at this stage would add significant operational complexity without proportional value, because:

1. The team is a single developer orchestrating AI agents — Kubernetes cluster management would consume time better spent on feature development
2. Docker Compose is sufficient for local development, integration testing (Testcontainers), and CI
3. No production deployment is planned until the core services (Identity, Wallet, BFF, Payment) are feature-complete
4. Helm charts require a running cluster to validate — premature chart authoring risks drift from the actual service topology

## Decision

**Defer Kubernetes manifests and Helm charts to a later phase.** The current infrastructure strategy is:

- **Local development**: Docker Compose (`infra/docker-compose.yml` + `docker-compose.dev.yml`)
- **CI**: GitHub Actions with Docker image builds (no cluster deployment)
- **Integration tests**: Testcontainers (real PostgreSQL + Kafka in test scope)
- **Future production**: Kubernetes + Helm when the platform reaches MVP status

When K8s/Helm is introduced, the following artifacts will be created:

- `infra/helm/aegis/` — Parent Helm chart with subcharts per service
- `infra/k8s/base/` — Raw Kubernetes manifests (Deployment, Service, ConfigMap, Secret)
- `infra/k8s/overlays/{dev,staging,prod}/` — Kustomize overlays for environment-specific configuration
- CI/CD pipeline updates for `helm upgrade --install` deployments with rolling updates

## Alternatives Considered

### Alternative 1: Create K8s manifests now (full implementation)
- **Pros**: Aligns with constitution; production-ready from day one
- **Cons**: Significant effort for services that are still evolving; manifests will need frequent updates as APIs change; no cluster to validate against

### Alternative 2: Docker Compose for everything (including production)
- **Pros**: Simple; no Kubernetes learning curve
- **Cons**: No auto-scaling, no self-healing, no rolling updates; not suitable for production fintech workloads

### Alternative 3: Defer and document (chosen)
- **Pros**: Focus on feature development; avoid premature infrastructure complexity; document the decision for future reference
- **Cons**: Constitution requirement not yet met; requires a follow-up ADR when K8s is introduced

## Consequences

### Positive
- Team focuses on delivering business value (features) over infrastructure
- Docker Compose remains the single source of truth for local development
- CI pipeline stays simple and fast

### Negative
- Constitution requirement for K8s+Helm is not yet met — this is a known gap
- When K8s is introduced, there will be a one-time effort to create charts and manifests

### Risks
- **Risk**: Forgetting to introduce K8s before production — **Mitigation**: This ADR should be revisited when the platform reaches MVP; a follow-up ADR will document the K8s adoption plan
- **Risk**: Docker Compose configuration drifts from future K8s manifests — **Mitigation**: Keep Docker Compose as the canonical local dev environment; K8s manifests will be derived from it

## Related Decisions

- ADR-002: Kafka topic configuration strategy (configuration-driven, not hardcoded)
- Constitution Principle: Kubernetes + Helm for production orchestration

## References

- `.specify/memory/constitution.md` — Infrastructure requirements
- `infra/docker-compose.yml` — Current Docker Compose configuration
- `infra/docker-compose.dev.yml` — Development overlay
