# ADR-013: Secrets Management — Externalized via Environment

## Status

Accepted

## Date

2026-08-07

## Context

Services need credentials (database passwords, JWT signing secrets, OTLP endpoint)
without hardcoding them in source. The repo is a public portfolio reference
architecture, so any committed secret is a security incident (Gitleaks scans enforce
this).

## Decision

**All secrets are externalized via environment variables and never committed.**

- Runtime credentials come from env vars with local defaults only for
  development-only, non-sensitive values (e.g. `DB_USERNAME:aegis`).
- Sensitive secrets (JWT signing keys, production DB passwords) have **no** default
  in source; they are supplied by the environment (compose files reference
  `.env`, Kubernetes uses Secrets/Argo CD).
- `.env` files are gitignored.
- A `.env.example` documents the required variables without values.
- CI enforces this with Gitleaks (fail on secrets) and Trivy secret scanning.

## Alternatives Considered

### Alternative 1: Secrets in application.yml
- **Pros**: simple.
- **Cons**: leaks into git; violates the public repo policy.

### Alternative 2: External secret manager (Vault)
- **Pros**: rotation, audit.
- **Cons**: operational overhead; appropriate for production, out of scope for DEV.

### Alternative 3: Kubernetes native Secrets
- **Pros**: native to the K8s deployment.
- **Cons**: still needs a source of truth; base64 is not encryption.

**Why not chosen**: environment variables are the correct baseline for a reference
architecture; production would layer a secret manager on top.

## Consequences

### Positive
- No secrets in the repository.
- Simple, portable configuration.

### Negative
- Operators must provision secrets per environment.

### Risks
- **Risk**: a developer hardcodes a default — **Mitigation**: Gitleaks + Trivy in
  the security workflow fail the build.

## Related Decisions

- Security workflow (Gitleaks, Trivy).

## References

- `.github/workflows/security.yml`
- `infra/.env.example`
- `SECURITY.md`
