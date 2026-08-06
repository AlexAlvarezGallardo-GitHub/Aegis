# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Repository governance: `CONTRIBUTING.md`, `.github/CODEOWNERS`, issue templates and PR template.
- Link checker (lychee) job in CI to validate internal and external links.
- Architecture tests (ArchUnit) enforcing hexagonal layer boundaries in every backend service.
- OpenAPI contract validation against controllers.
- Mutation testing (PIT) coverage reporting for critical domain modules.
- Public coverage badge wired to a dedicated `coverage.yml` workflow.

## [0.1.0] - 2026-08-06

### Added

- **Identity Service** (`aegis-identity-service`): user registration (UC-001), user authentication (UC-002) with JWT, RBAC, BCrypt password hashing, transactional outbox and Flyway migrations.
- **BFF Service** (`aegis-bff-service`): backend-for-frontend with HttpOnly session cookies, JWT proxy, CSRF protection and Redis session store.
- **Wallet Service** (`aegis-wallet-service`): wallet creation (UC-003), deposit funds (UC-004) with idempotency keys, manage wallet with adjust balance and premium.
- **Fraud Service** (`aegis-fraud-service`): real-time fraud detection (UC-008) with configurable rule engine and risk scoring.
- **Audit Service** (`aegis-audit-service`): immutable audit trail built from domain events.
- **Reporting Service** (`aegis-reporting-service`): event consumers and projections (reporting capabilities partial).
- **Common library** (`aegis-common`): UUID v7 generator, shared base exceptions, utilities.
- **Frontend** (`aegis-frontend`, Angular 22 + Material): registration, login, wallet management, dashboard with KPIs, design system with dark/light theme and WCAG 2.1 AA accessibility.
- **Infrastructure**: hot-reload dev environment (Docker Compose + Spring DevTools), production multi-stage builds, OpenTelemetry instrumentation, observability stack (Prometheus, Grafana, Tempo, Loki).
- **CI/CD**: GitHub Actions with branch naming, commit message, PR title/body validation, backend matrix builds with JaCoCo coverage (80% line threshold), frontend lint/build/tests, security pipeline (Gitleaks, CodeQL, Trivy FS/IaC, OpenSSF Scorecard, SBOM), release pipeline with GHCR images and SBOMs.
- **GitOps**: separate `Aegis-GitOps` repository with Helm charts, Argo CD app-of-apps structure and image tag promotion.
- **Security**: `SECURITY.md`, dependency updates via Dependabot, Gitleaks secret scanning, externalized secrets via environment variables.
- **Documentation**: canonical service catalog, capability × environment matrix, ADRs, OpenAPI contracts, Obsidian vault with domain events, ports and infrastructure diagrams, engineering portfolio evidence.
- **Portfolio data**: GitHub metrics workflow (`update-github-metrics.yml`) generating real repository metrics consumed by the portfolio site.

### Changed

- Checkstyle upgraded to 10.17.0 and enforced at `verify` phase across all backend modules.
- Security tooling upgraded to GitHub Actions v4 (CodeQL), Trivy filesystem/IaC scans and Scorecard 2.3.3.
- Frontend migrated to a design-system-based architecture with shared components and design tokens.

### Fixed

- Bash command injection from backticks in PR body validation.
- Actuator health endpoint availability and auth bypass for health checks.
- Integration test and frontend build failures in CI.
- Checkstyle violations across all services.

### Security

- Hardcoded secrets externalized to environment variables.
- Duplicate email handled as conflict instead of data integrity error.
- Gitleaks + Trivy secret scanning wired into the security pipeline.

### Known limitations

- Only Local and DEV environments are functional; PRE, STAGE and PROD are prepared structures.
- No real customer traffic, regulatory certification, banking or KYC provider integration.
- Reporting service consumers and projections are incomplete.
- No commercial SLA; observability evidence captured from the local stack.

[0.1.0]: https://github.com/AlexAlvarezGallardo-GitHub/Aegis/releases/tag/v0.1.0
