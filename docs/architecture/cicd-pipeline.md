# CI/CD Pipeline

> **Status:** Documented. The pipeline runs on GitHub Actions (`.github/workflows/`)
> and promotes Docker images + GitOps overlays to the DEV environment.

## Flow

```mermaid
graph LR
    PR[Push / PR] --> DETECT[detect-changes]
    DETECT --> PV[PR Validation]
    DETECT --> CI[CI]

    subgraph PR_Validation[pr-validation.yml]
        PV --> BN[Branch Name]
        PV --> CM[Commit Messages]
        PV --> PT[PR Title]
        PV --> PB[PR Body]
        PV --> BB[Backend Build]
        PV --> FB[Frontend Build]
        PV --> FU[Frontend Unit Tests]
        PV --> LC[Link Checker]
    end

    subgraph CI[ci.yml]
        CI --> BACK[Backend Full Build + coverage]
        CI --> IT[Backend Integration Tests]
        CI --> MUT[Mutation Testing]
        CI --> FRONT[Frontend Build + Tests]
        CI --> DOCKER[Docker Build + Push GHCR]
        CI --> SBOM[SBOM]
        CI --> SEC[Security Scan]
        CI --> SIGN[Cosign Sign]
        CI --> GITOPS[GitOps Update via PR]
    end

    DOCKER --> IMG[(GHCR images)]
    GITOPS --> GPR[PR to Aegis-GitOps]
    GPR --> HELM[Helm template validation]
    HELM --> MERGE[Squash merge]
    MERGE --> ARGO[Argo CD sync DEV]

    DETECT --> SECW[security.yml]
    subgraph SECW[Security]
        SECW --> GL[Gitleaks]
        SECW --> TR[Trivy FS/IaC]
        SECW --> SC[Scorecard]
    end

    style PR fill:#bbf,color:#000
    style CI fill:#bbf,color:#000
    style GITOPS fill:#fdb,color:#000
    style ARGO fill:#fdb,color:#000
```

## Stage-by-stage

| Stage | Workflow | Artifact / Effect | Fail condition |
|-------|----------|-------------------|----------------|
| Branch/commit/title checks | `pr-validation.yml` | PR blocked | Convention violation |
| Backend build | `pr-validation.yml` / `ci.yml` | JaCoCo coverage ≥ 80% | Coverage below threshold |
| Integration tests | `ci.yml` | Testcontainers pass | Any IT failure |
| Mutation testing | `ci.yml` | PIT report artifact | (report only) |
| Frontend build/lint/tests | `ci.yml` | Dist bundle | Lint/build/test failure |
| Docker build | `ci.yml` | GHCR image `:sha` | Build failure |
| SBOM + signing | `ci.yml` | SBOM + cosign signature | Verification failure |
| Security | `security.yml` | SARIF alerts | Gitleaks/Trivy findings |
| GitOps promotion | `ci.yml` `gitops-update` | PR to `Aegis-GitOps` → squash merge | Helm validation failure |

## Responsible parties

- **Developer**: owns the PR, the tests, and the merge (with review).
- **CI**: enforces quality gates and promotes images.
- **Argo CD**: applies the GitOps overlay to the DEV cluster (GitOps reconciliation).

## Evidence

- Workflow definitions: `.github/workflows/*.yml`
- Coverage badge: `README.md`
- Release: `release.yml` → GitHub Release + GHCR

## See also

- [GitOps](../obsidian/05%20-%20Infrastructure/GitOps.md)
- [Argo CD](../obsidian/05%20-%20Infrastructure/Argo%20CD.md)
- `CHANGELOG.md` (release notes per version)
