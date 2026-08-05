# Aegis - AI Engineering Conventions

## Project Overview

Aegis is a digital payment platform reference architecture built with Java 21, Spring Boot 3, Angular, Kafka, and Kubernetes. It follows microservices architecture with event-driven communication.

## Hexagonal Architecture

Every service MUST follow this package structure (rules defined in `.specify/memory/constitution.md` Principle I):

```
com.aegis.<service>/
├── domain/
│   ├── model/          (entities, value objects, enums)
│   ├── event/          (domain events)
│   ├── exception/      (domain exceptions)
│   └── port/
│       ├── inbound/    (use case interfaces)
│       └── outbound/   (repository, gateway interfaces)
├── application/
│   ├── service/        (use case implementations)
│   ├── mapper/         (DTO <-> Domain mappers)
│   └── dto/            (data transfer objects)
├── infrastructure/
│   ├── persistence/    (JPA entities, repositories, adapters)
│   ├── messaging/      (Kafka producers/consumers)
│   ├── config/         (Spring configuration)
│   └── security/       (security config)
└── web/
    ├── controller/     (REST controllers)
    ├── advice/         (exception handlers)
    └── filter/         (request filters)
```

## Java Conventions

- Use records for immutable DTOs and value objects
- Use `@Value` for immutable Spring beans
- Use `@Entity` only in infrastructure layer

## API Documentation

- OpenAPI 3 specs MUST be defined in separate YAML files under `specs/<feature>/contracts/`.
- Controllers MUST NOT contain swagger/OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, etc.). These duplicate the YAML spec and add complexity without gain.
- The `api-design` skill generates the OpenAPI contract. The `service-builder` agent must produce controllers with zero swagger imports.

## Build Commands

- Build all: `mvn clean install`
- Run tests: `mvn test`
- Run integration tests: `mvn verify -Pintegration-tests`
- Lint/Checkstyle: `mvn checkstyle:check`
- Frontend build: `npm run build` (in `aegis-frontend/`)
- Frontend lint: `npm run lint` (in `aegis-frontend/`)
- Frontend test: `npm run test` (in `aegis-frontend/`)

## Git Workflow (GitHub Flow)

### Branching
- `main` is the only long-lived branch and must always be deployable.
- Create feature branches from `main`, merge back via pull requests.
- Branch naming: `<type>/<number>-<short-description>` (lowercase kebab-case).
- Types: `feature/`, `fix/`, `chore/`, `refactor/`, `docs/`, `test/`, `ci/`, `security/`
- `<number>` is the sequential feature number from `specs/<number>-<short-name>/`.
- Example: `feature/001-user-registration`, `fix/042-jwt-refresh-rotation`

### Commit Messages
Format: `<type>(<scope>): <description>`

- **Types**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`, `perf`, `security`
- **Scopes**: `identity`, `wallet`, `payment`, `fraud`, `notification`, `audit`, `reporting`, `gateway`, `infra`, `frontend`
- Description: lowercase, imperative mood, no period, max 72 chars.
- Breaking changes: add `!` after scope or `BREAKING CHANGE:` in body.
- Reference issues in footer: `Closes #42`

### Pull Requests
- PR title follows the same commit message format.
- PR body must include: Summary, Changes, Testing, and Checklist.
- One PR per feature/fix. Keep PRs small and focused (< 400 lines preferred).
- Squash merge to keep history clean.
- All CI checks must pass. At least one approval required.

### Pre-Commit Validation
Git hooks in `.githooks/` enforce branch naming convention locally and block commits from non-main branches that do not match `<type>/<number>-<short-description>`. Enable with:
```
git config core.hooksPath .githooks
```

## OpenCode Plugins

Plugins in `.opencode/plugins/` run automatically during agent sessions to enforce Aegis conventions:

| Plugin | Trigger | Purpose |
|--------|---------|---------|
| `pre-edit-guard.ts` | Before `edit`/`write` on `.java`/`.kt` files | Blocks forbidden imports in domain and controller layers (hexagonal architecture enforcement) |
| `pre-commit-guard.ts` | Before `git commit` bash calls | Warns when commit messages do not follow `<type>(<scope>): <description>` and when staged changes contain potential secrets |
| `post-edit-security.ts` | After `edit`/`write` on source/config files | Warns about hardcoded secrets, insecure URLs, sensitive log data, `System.out`, and `printStackTrace` |
| `post-scaffold.ts` | After `write`/`edit` of service `pom.xml` | Suggests follow-up skills/agents (ADR, api-design, event-design, test-engineer, infra-engineer, security-reviewer, architect) |

Plugins emit warnings; blocking behavior for architecture violations is handled by Checkstyle in CI and by the `pre-commit` git hook locally.

## Issue Management

The `issue-manager` agent owns all GitHub issue lifecycle operations. Use it for every issue-related task.

### Workflow

1. **Plan** — Before starting work, ask the issue-manager to create an epic or feature issue with sub-tasks.
2. **Track** — Reference the issue number in branch names and commits: `Closes #42`.
3. **Sync** — After completing sub-tasks, ask the issue-manager to sync parent task lists and unblock dependents.
4. **Report** — Request status reports to track epic progress, blockers, and stale issues.

### Common Requests

| Request | What it does |
|---------|--------------|
| `Create epic for [feature]` | Creates epic + sub-issues per service/task, links them |
| `Break down #N` | Reads issue, creates sub-tasks, updates parent task list |
| `Sync issues` | Fixes broken links, updates task lists, checks label consistency |
| `What's blocked?` | Lists all issues waiting on unresolved dependencies |
| `Status report` | Generates epic progress, blocked items, stale issues, risks |
| `Triage` | Classifies unlabeled issues, applies labels, suggests assignees |
| `Link #A to #B` | Updates both issues with cross-references |
| `Close #N` | Verifies children closed, updates parent, closes issue |

### Issue Hierarchy

```
Epic
 ├── Feature / Story
 │    ├── Sub-task (implementation)
 │    ├── Sub-task (tests)
 │    └── Sub-task (documentation)
 ├── Bug
 └── Tech Debt
```

- Every issue MUST be linked to a parent or be an epic (no orphans).
- Parent issues maintain a GitHub task list of all children.
- Closing a child automatically updates the parent's checklist.
- Priority cascades: critical epics require at least high-priority children.
- Cross-service dependencies are explicitly tracked with `Depends on` / `Blocks`.

## Frontend Testing Conventions

When testing Angular components, the test-engineer MUST verify:

### UI Interaction Patterns

- **Submit button disabled state**: Test that the submit button has `[disabled]="isLoading || form.invalid"` and verify both conditions keep it disabled
- **Validation feedback**: Test that submitting an invalid form shows a snackbar/toast (not just field-level errors)
- **Loading spinner reset**: Test that `isLoading` resets via `finalize()` on success, error, AND if the observable never emits (timeout). Do NOT rely on `next`/`error` handlers alone for `isLoading` reset
- **HTTP timeout**: Verify the service or component handles HTTP timeouts gracefully (no infinite spinner)

### HTTP & API Patterns

- **Relative URLs**: Services MUST use relative paths (`/api/v1/...`) through the Angular proxy, NOT absolute URLs (`http://localhost:8081/...`), unless a BFF or API gateway is configured
- **Error handling**: Test both HTTP 4xx and 5xx error responses, and network errors (HttpErrorResponse without status)
- **Proxy configuration**: Verify `proxy.conf.json` is properly configured and services route through it in dev

### Test Structure

- **Button disabled**: `expect(button.nativeElement.disabled).toBeTruthy()` when form invalid
- **Snackbar on validation fail**: Spy on `MatSnackBar.open()` and verify it's called when submitting invalid form
- **finalize guarantee**: Use a spy and verify `isLoading` transitions: `false → true → false` always completes
- **HTTP mock**: Use `HttpTestingController` to flush/expect requests and verify loading states

## Mermaid Diagrams (Vault + Service READMEs)

The Mermaid rules below apply to `docs/obsidian/` files AND to the `## Architecture` section of every service `README.md` (`backend/*/README.md`). No ASCII/plain-text diagrams allowed anywhere.

Every service documentation file in `docs/obsidian/01 - Services/` MUST include:
- A `graph` diagram showing the service's hexagonal architecture (layers → ports → infrastructure)
- A `sequenceDiagram` showing the primary flow (request → domain → event → downstream consumers)

Every domain event file in `docs/obsidian/03 - Domain Events/` MUST include:
- A `graph LR` showing producer → topic → consumers
- A `sequenceDiagram` showing the event publication flow (domain → outbox → kafka → consumers)

Every inbound port file in `docs/obsidian/04 - Ports/inbound/` MUST include:
- A `sequenceDiagram` showing the use case flow with validation branches, exceptions, and event publication

Every infrastructure file (Kafka Topics, etc.) MUST include:
- A `graph` diagram showing the topology (topics, partitions, consumer groups)

Every enum/status domain model file (UserStatus, WalletStatus, etc.) MUST include:
- A `stateDiagram-v2` showing state transitions (with trigger labels)

No file in the vault MAY use plain-text/ASCII diagrams (` ```text `) for architecture, flows, or transitions — Mermaid is mandatory.

All Mermaid diagrams MUST use ` ```mermaid ` blocks (Obsidian-native rendering), not inline diagrams. Use `graph TB`/`graph LR` for static topology and `sequenceDiagram` for flows. Color nodes with `style` directives for visual grouping: services `#bbf`, infrastructure `#fdb`, databases `#afa`.

When using `subgraph` blocks, ALL nodes belonging to a subgraph MUST be declared (with their shape and label) INSIDE that subgraph, then referenced by id from outside. Never create nodes in top-level arrow statements and later list them inside a subgraph — that breaks rendering in some Mermaid versions.

## Authority

All architectural principles, naming conventions, API design standards, security requirements, testing standards, and infrastructure constraints are defined in `.specify/memory/constitution.md`. That document supersedes all other development guidance.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
