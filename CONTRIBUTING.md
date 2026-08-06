# Contributing to Aegis

Thank you for your interest in contributing to Aegis. This guide defines how to
contribute, what is expected from you, and how your changes are reviewed and merged.

Aegis is a **reference architecture** project: everything in this repository is
evidence-driven. Every feature is specified before it is built, and every claim in
the documentation is backed by a link. Please respect this philosophy.

---

## Table of Contents

1. [Development Requirements](#development-requirements)
2. [Project Conventions](#project-conventions)
3. [Branches](#branches)
4. [Commit Messages](#commit-messages)
5. [Pull Requests](#pull-requests)
6. [Running Tests and Quality Gates](#running-tests-and-quality-gates)
7. [Definition of Done](#definition-of-done)
8. [Dependency Policy](#dependency-policy)
9. [AI-Assisted Development](#ai-assisted-development)
10. [Getting Help](#getting-help)

---

## Development Requirements

| Tool | Version | Purpose |
|------|---------|---------|
| JDK | 21 | All backend services |
| Maven | 3.9+ | Backend build and test orchestration |
| Node.js | 22 | Angular frontend build |
| npm | 10+ | Frontend dependency management |
| Docker | 24+ | Local stack, Testcontainers integration tests |

### First-time setup

```bash
# Enable local git hooks (branch naming + commit message validation)
git config core.hooksPath .githooks

# Copy the environment template and generate your own dev secrets
copy infra\.env.example infra\.env   # Windows
# cp infra/.env.example infra/.env   # Linux/macOS
```

`infra/.env` is gitignored and holds **development-only** credentials. Never
commit it and never reuse its values outside local development.

---

## Project Conventions

Read the full engineering conventions before contributing:

- **Architecture rules and constitution**: [`.specify/memory/constitution.md`](.specify/memory/constitution.md)
- **AI agent workflow**: [`docs/AGENTS-README.md`](docs/AGENTS-README.md)
- **Canonical service catalog**: [`docs/architecture/service-catalog.md`](docs/architecture/service-catalog.md)
- **Project status matrix**: [`docs/project-status.md`](docs/project-status.md)

### Non-negotiable conventions

- **Hexagonal architecture**: every backend service follows the
  `domain / application / infrastructure / web` package structure. `domain`
  must stay free of any Spring or JPA imports (enforced by ArchUnit and Checkstyle).
- **Spec-first**: OpenAPI contracts live in `specs/<feature>/contracts/`. Controllers
  must NOT contain swagger annotations — they duplicate the YAML spec.
- **No hardcoded secrets**: all credentials come from environment variables.
- **Mermaid, not ASCII**: all architecture and flow diagrams use ` ```mermaid ` blocks.

---

## Branches

- `main` is the only long-lived branch and must always be deployable.
- Create feature branches from `main` and merge via pull request.
- Branch naming: `<type>/<number>-<short-description>` (lowercase kebab-case).

| Type | Use for |
|------|---------|
| `feature/` | New functionality |
| `fix/` | Bug fixes |
| `chore/` | Maintenance, dependencies, tooling |
| `refactor/` | Code restructuring without behavior change |
| `docs/` | Documentation-only changes |
| `test/` | Test additions or changes |
| `ci/` | CI/CD pipeline changes |
| `security/` | Security hardening |

`<number>` is the sequential feature number from `specs/<number>-<short-name>/`.

```bash
git checkout -b feature/001-user-registration
```

---

## Commit Messages

Format: `<type>(<scope>): <description>`

```text
feat(wallet): add idempotent deposit with unique reference
fix(identity): handle duplicate email as conflict
docs(infra): document outbox failure recovery
```

- **Types**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`, `perf`, `security`
- **Scopes**: `identity`, `wallet`, `payment`, `fraud`, `notification`, `audit`,
  `reporting`, `gateway`, `infra`, `frontend`
- Description: lowercase, imperative mood, no period, max 72 chars.
- Breaking changes: add `!` after the scope or `BREAKING CHANGE:` in the body.
- Reference issues in the footer: `Closes #42`.

These rules are enforced by the local git hook and by CI.

---

## Pull Requests

- **One PR per feature/fix.** Keep PRs small and focused (prefer < 400 changed lines).
- PR title follows the same `<type>(<scope>): <description>` format as commits.
- PR body **must** include the sections from `.github/pull_request_template.md`:
  Summary, Changes, Testing, and Checklist.
- At least **one approval** is required before merging.
- All CI checks must pass.
- Merge with **squash** to keep history clean.

> The PR Validation workflow runs branch naming, commit message, PR title and
> PR body checks, plus backend builds, frontend lint/build/tests and a link
> checker. A PR that fails validation cannot be merged.

---

## Running Tests and Quality Gates

### Backend

```bash
# Build everything (unit tests + checkstyle + coverage)
cd backend
mvn clean verify

# Run only unit tests
mvn test

# Run integration tests (Testcontainers spins up real PostgreSQL + Kafka)
mvn verify -Pintegration-tests

# Lint / Checkstyle only
mvn checkstyle:check
```

Backend coverage is enforced at **80% line coverage** per module (JaCoCo).

### Frontend

```bash
cd frontend/aegis-frontend
npm install

npm run build    # production build
npm run lint     # ESLint
npm run test     # Jasmine unit tests (ChromeHeadless)
```

### End-to-end

Playwright tests live in `e2e/`. See `e2e/README.md` for setup.

> Integration and end-to-end tests require Docker.

---

## Definition of Done

A change is considered done when **all** of the following are true:

- [ ] Feature specification exists in `specs/<number>-<name>/` (for new features)
- [ ] OpenAPI contract updated in `specs/<feature>/contracts/` where applicable
- [ ] Architecture decision documented as an ADR where a decision was made
- [ ] Code follows hexagonal architecture and passes Checkstyle
- [ ] Domain layer has no framework dependencies (ArchUnit verifies)
- [ ] Unit tests cover domain and application logic
- [ ] Integration tests updated for affected adapters
- [ ] Database migrations included and backward compatible where applicable
- [ ] Security review performed (no secrets, no new vulnerabilities)
- [ ] Observability: metrics/traces/logs considered for new operations
- [ ] Documentation kept in sync (service catalog, project status, vault)
- [ ] CI is green

---

## Dependency Policy

- Dependency updates are handled by **Dependabot** (Maven, npm, GitHub Actions).
- Open PRs are grouped by ecosystem to reduce noise.
- **Patch and minor** updates: reviewed and merged as regular PRs.
- **Major** updates: require explicit review of migration notes and a decision
  recorded in the PR (ADR where it changes architecture).
- Do not add a new dependency without documenting the rationale in the PR.

---

## AI-Assisted Development

Aegis is built through an **AI-assisted engineering workflow with human ownership**
of architecture, validation and technical decisions. If you use AI tools:

- **You are responsible** for every line of code merged, even AI-generated ones.
- Validate AI output against the project conventions and the constitution.
- Never paste secrets, tokens or personal data into AI prompts.
- Record the use of AI agents in the PR description where significant.
- AI-generated code must pass the same quality gates as hand-written code.

See [`docs/ai-engineering-governance.md`](docs/ai-engineering-governance.md) for details.

---

## Getting Help

- Architecture rules: `.specify/memory/constitution.md`
- Issue tracker: [GitHub Issues](https://github.com/AlexAlvarezGallardo-GitHub/Aegis/issues)
- Security reports: see [`SECURITY.md`](SECURITY.md)

Please be respectful and constructive. This is a portfolio reference architecture,
not a commercial product — feedback that improves engineering quality is always welcome.
