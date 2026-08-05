# Security Policy

Aegis is a demonstration platform, but it applies **defense-in-depth security** throughout the software supply chain — because the practices shown here are exactly what fintech systems require.

## Supported Versions

Aegis is continuously developed. Only the latest commit on `main` receives security fixes. Tagged releases, once introduced, will receive fixes for the latest minor version.

| Version | Supported |
|---------|-----------|
| `main` (latest) | :white_check_mark: |
| Previous releases | :x: |

## Reporting a Vulnerability

Please **do not open a public issue** for security problems.

If you find a vulnerability, use one of these private channels:

1. **GitHub Private Vulnerability Reporting** (preferred) — the repository enables the "Security" tab → "Report a vulnerability" flow.
2. **Email** the maintainer directly: `alexag1999@gmail.com`.

### What to include

- Affected component/service and version (commit SHA if possible).
- Type of vulnerability and severity assessment.
- Steps to reproduce (minimal, no production data).
- Any proof-of-concept you are comfortable sharing.

### What happens next

- Acknowledgment within **48 hours**.
- Confirmation and triage within **5 business days**.
- A coordinated disclosure timeline is agreed before any public notice.
- The reporter is credited (unless they prefer anonymity).

## Security Features & Tooling

Aegis automates the following security checks in CI/CD (see `.github/workflows/` and `docs/obsidian/05 - Infrastructure/Security & Supply Chain.md`):

| Layer | Tool | When |
|-------|------|------|
| Secret scanning | **Gitleaks** | Every PR + weekly scheduled scan |
| Static analysis (SAST) | **CodeQL** (Java + TypeScript) | Weekly + on-demand |
| Dependency / filesystem scan | **Trivy** | Weekly scheduled scan |
| Container image scan | **Trivy** → SARIF to Security tab | Every merge to `main` |
| Infrastructure as Code scan | **Trivy (IaC)** | Weekly scheduled scan |
| SBOM generation | **Syft** (CycloneDX) | Every merge to `main` |
| Artifact signing | **Cosign** (keyless via GitHub OIDC) | Every merge to `main` |
| Dependency updates | **Dependabot** + **Renovate** | Continuous |
| Supply-chain score | **OpenSSF Scorecard** | Weekly scheduled scan |

## Security Best Practices Applied

- **Secrets**: never committed. All secrets live in GitHub Actions secrets / environment stores.
- **Authentication**: BCrypt (cost ≥ 10) for credentials; JWT with rotation; HttpOnly session cookies through the BFF.
- **Authorization**: RBAC enforced per service; API Gateway centralizes rate limiting and circuit breaking.
- **Infrastructure**: distroless container images (minimal attack surface), keyless image signing, GitOps deployments with immutable tags.

## Disclosure Policy

This is a personal/portfolio codebase. We follow a responsible-disclosure model: no exploit hunting outside your own deployments, no disclosure to third parties until the maintainer has had a reasonable window to fix the issue.
