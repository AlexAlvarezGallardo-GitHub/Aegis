# Aegis - Agents & Workflows

This document describes the AI agent system, git workflow, and CI/CD pipeline for the Aegis digital payment platform.

## Agent Overview

Aegis uses a set of specialized AI agents configured in `opencode.json`. Each agent handles a specific domain of the development lifecycle.

Agents are tiered by cost to keep token usage low. The main session model and `small_model` (titles/lightweight tasks) are set in `opencode.json`.

| Agent | Model | Tier | Role |
|-------|-------|------|------|
| _main session_ | `opencode-go/deepseek-v4-flash` | flash | Default agent for every turn |
| _small_model_ | `opencode/deepseek-v4-flash-free` | free | Titles and lightweight tasks |
| `explore` | `opencode/deepseek-v4-flash-free` | free | Fast codebase search and file discovery |
| `scout` | `opencode/deepseek-v4-flash-free` | free | Open-ended exploration across multiple locations |
| `plan` | `opencode-go/qwen3.7-plus` | plus | Planning and breakdown |
| `general` | `opencode/deepseek-v4-flash-free` | free | General-purpose multi-step research and execution |
| `issue-manager` | `opencode/deepseek-v4-flash-free` | free | Manages GitHub issue lifecycle: triage, linking, sync, status reports |
| `architect` | `opencode-go/qwen3.7-plus` | plus | Validates DDD boundaries, microservice decomposition, C4 models |
| `security-reviewer` | `opencode-go/deepseek-v4-flash` | flash | Reviews OAuth2/JWT, scans secrets, OWASP compliance |
| `code-reviewer` | `opencode-go/qwen3.7-plus` | plus | Enforces SOLID, Clean Code, hexagonal architecture |
| `service-builder` | `opencode-go/qwen3.7-plus` | plus | Generates Spring Boot microservices and YAML-first OpenAPI contracts |
| `frontend-builder` | `opencode-go/qwen3.7-plus` | plus | Generates Angular components and services |
| `test-engineer` | `opencode-go/qwen3.7-plus` | plus | Generates JUnit 5, Mockito, Testcontainers tests; Playwright only for real E2E |
| `infra-engineer` | `opencode-go/qwen3.7-plus` | plus | Creates Docker, Kubernetes, Helm, GitHub Actions |
| `reporter` | `opencode/deepseek-v4-flash-free` | free | Creates GitHub issues (bugs, features, tech debt) |
| `git-guardian` | `opencode/deepseek-v4-flash-free` | free | Enforces branch naming, commits, and PR conventions |

**Tier guide:**
- `free` — routine work (search, triage, git/issue ops). No token cost.
- `flash` — main session and security review. Cheap, good for most turns.
- `plus` — planning, architecture, builders, tests, infra. Use when quality matters.

**Token-saving policy:** `compaction.prune` is enabled and `watcher.ignore` excludes `node_modules/`, `target/`, `dist/`, and `.opencode/node_modules/` so searches and the file watcher do not ingest build artifacts or vendored JS.

### Agent Routing

```mermaid
flowchart TD
    TASK[Task Arrives] --> PHASE{Which Phase?}

    PHASE -->|Planning| PLAN_ROUTE{Issue Work?}
    PLAN_ROUTE -->|Create epic/feature| IM[issue-manager]
    PLAN_ROUTE -->|Break down issue| IM2[issue-manager]
    PLAN_ROUTE -->|Triage / status| IM3[issue-manager]

    PHASE -->|Setup| SETUP_ROUTE{Task Type?}
    SETUP_ROUTE -->|Docker/Helm/K8s| INFRA[infra-engineer]
    SETUP_ROUTE -->|Maven/Packages| SB[service-builder]

    PHASE -->|Foundational| SB

    PHASE -->|User Story| STORY_ROUTE{Task Type?}
    STORY_ROUTE -->|Angular/UI| FB[frontend-builder]
    STORY_ROUTE -->|Kafka/Events| SB2[service-builder + event-design]
    STORY_ROUTE -->|REST/API| SB3[service-builder + api-design]
    STORY_ROUTE -->|Swagger/OpenAPI| SB5[service-builder + api-design]
    STORY_ROUTE -->|Entity/Model/Service| SB
    STORY_ROUTE -->|Test/Coverage| TE[test-engineer]
    STORY_ROUTE -->|Security/OAuth2| SB4[service-builder then security-reviewer]

    PHASE -->|Polish| POLISH_ROUTE{Task Type?}
    POLISH_ROUTE -->|Tests/Coverage| TE
    POLISH_ROUTE -->|Code Quality| CR[code-reviewer]
    POLISH_ROUTE -->|Security Audit| SR[security-reviewer]
    POLISH_ROUTE -->|Architecture| AR[architect]

    PHASE -->|Close| CLOSE_ROUTE{Issue Done?}
    CLOSE_ROUTE -->|Close + sync| IM4[issue-manager]
    CLOSE_ROUTE -->|PR validation| GG[git-guardian]

    style IM fill:#f59e0b,color:#fff
    style IM2 fill:#f59e0b,color:#fff
    style IM3 fill:#f59e0b,color:#fff
    style IM4 fill:#f59e0b,color:#fff
    style INFRA fill:#3b82f6,color:#fff
    style SB fill:#22c55e,color:#fff
    style SB2 fill:#22c55e,color:#fff
    style SB3 fill:#22c55e,color:#fff
    style SB4 fill:#22c55e,color:#fff
    style SB5 fill:#22c55e,color:#fff
    style FB fill:#06b6d4,color:#fff
    style TE fill:#a855f7,color:#fff
    style CR fill:#eab308,color:#fff
    style SR fill:#ef4444,color:#fff
    style AR fill:#8b5cf6,color:#fff
    style GG fill:#22d3ee,color:#fff
```

## Git Workflow (GitHub Flow)

All development follows GitHub Flow with strict naming conventions.

```mermaid
gitGraph
    commit id: "Initial commit"
    branch feature/001-user-registration
    checkout feature/001-user-registration
    commit id: "feat(identity): add user registration endpoint"
    commit id: "test(identity): add registration service tests"
    checkout main
    merge feature/001-user-registration id: "squash merge 1" tag: "v1.1.0"
    branch fix/042-jwt-refresh
    checkout fix/042-jwt-refresh
    commit id: "fix(identity): correct token rotation"
    checkout main
    merge fix/042-jwt-refresh id: "squash merge 2" tag: "v1.1.1"
```

### Branch Naming

```
<type>/<number>-<short-description>
```

- `<type>` follows GitHub Flow conventions.
- `<number>` is the sequential feature number from `specs/<number>-<short-name>/`.
- `<short-description>` is lowercase kebab-case.

| Type | Purpose |
|------|---------|
| `feature/` | New functionality |
| `fix/` | Bug fixes |
| `chore/` | Maintenance, tooling |
| `refactor/` | Code restructuring |
| `docs/` | Documentation |
| `test/` | Test additions |
| `ci/` | CI/CD changes |
| `security/` | Security fixes |

### Commit Messages

```
<type>(<scope>): <description>
```

| Type | Scope |
|------|-------|
| `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`, `perf`, `security` | `identity`, `wallet`, `payment`, `fraud`, `notification`, `audit`, `reporting`, `gateway`, `infra`, `frontend` |

## Enforcement Pipeline

Three layers of enforcement ensure conventions are followed at every stage:

```mermaid
flowchart LR
    subgraph LOCAL["Local - Git Hooks"]
        direction TB
        HOOK1["pre-commit\nBranch name check"]
        HOOK2["commit-msg\nMessage format check"]
        HOOK1 --> HOOK2
    end

    subgraph AI["AI Agent - git-guardian"]
        direction TB
        GG1["Branch validation"]
        GG2["Commit validation"]
        GG3["PR review"]
        GG1 --> GG2 --> GG3
    end

    subgraph CI["GitHub Actions - pr-validation.yml"]
        direction TB
        CI1["Branch name job"]
        CI2["Commit messages job"]
        CI3["PR title job"]
        CI4["PR body job"]
        CI1 --> CI2 --> CI3 --> CI4
    end

    LOCAL -->|"commit"| AI
    AI -->|"push + open PR"| CI

    style LOCAL fill:#1e293b,color:#fff
    style AI fill:#0f172a,color:#fff
    style CI fill:#0c0a09,color:#fff
```

### Git Hooks

Located in `.githooks/`. Enable with:

```bash
git config core.hooksPath .githooks
```

| Hook | Trigger | Action |
|------|---------|--------|
| `pre-commit` | Before each commit | Warns if branch name does not follow convention |
| `commit-msg` | After writing message | **Blocks** commit if message does not match format |

### GitHub Actions - PR Validation

Triggered on every PR event (`opened`, `edited`, `synchronize`, `reopened`):

```mermaid
flowchart TD
    PR[PR Opened or Updated] --> JOBS{4 Parallel Jobs}

    JOBS --> J1["validate-branch-name\nChecks type/description format"]
    JOBS --> J2["validate-commit-messages\nChecks all commits in PR"]
    JOBS --> J3["validate-pr-title\nChecks conventional commit format"]
    JOBS --> J4["validate-pr-body\nChecks Summary/Changes/Testing"]

    J1 --> RESULT{All Pass?}
    J2 --> RESULT
    J3 --> RESULT
    J4 --> RESULT

    RESULT -->|Yes| MERGE[PR can be merged]
    RESULT -->|No| BLOCK[PR blocked from merge]

    style MERGE fill:#22c55e,color:#fff
    style BLOCK fill:#ef4444,color:#fff
```

## Specification-Driven Development

Features follow the Specify lifecycle managed by speckit workflows:

```mermaid
flowchart LR
    C[Constitution] --> S[Specify]
    S --> CL[Clarify]
    CL --> P[Plan]
    P --> T[Tasks]
    T --> I[Issues]
    I --> A[Analyze]
    A --> CK[Checklist]
    CK --> IM[Implement]
    IM --> CL2[Close]

    CL2 --> REVIEW{Post-Implementation Review}
    REVIEW --> AR[architect]
    REVIEW --> CR[code-reviewer]
    REVIEW --> SR[security-reviewer]

    I -.->|issue-manager creates| ISSUES[GitHub Issues & Sub-Tasks]
    IM -.->|issue-manager syncs| SYNC[Task Lists & Dependencies]
    CL2 -.->|issue-manager closes| CLOSE[Close Issues & Update Epics]

    style C fill:#8b5cf6,color:#fff
    style IM fill:#22c55e,color:#fff
    style AR fill:#8b5cf6,color:#fff
    style CR fill:#eab308,color:#fff
    style SR fill:#ef4444,color:#fff
    style ISSUES fill:#f59e0b,color:#fff
    style SYNC fill:#f59e0b,color:#fff
    style CLOSE fill:#f59e0b,color:#fff
```

## Quick Reference

### Using the issue-manager agent

The `issue-manager` agent is your project manager for all GitHub issue operations. Use it at every stage of the development lifecycle.

**Before starting work:**
- "Create epic for [feature name]" — scaffolds the epic with sub-issues per service, links them all
- "Break down #N" — decomposes a feature into implementation, test, and documentation sub-tasks

**While working:**
- "Sync issues" — fixes broken links, updates parent task lists, checks label consistency
- "What's blocked?" — lists all issues waiting on unresolved dependencies
- "Link #A to #B" — creates cross-references between related issues

**After completing work:**
- "Close #N" — verifies all children are closed, updates parent checklist, closes issue
- "Status report" — generates epic progress table, blocked items, stale issues, risk flags

**Maintenance:**
- "Triage" — classifies unlabeled/unassigned issues, applies labels, suggests assignees
- "Prioritize" — sorts open issues by impact/urgency matrix, suggests reordering

#### Issue Lifecycle

```mermaid
flowchart LR
    PLAN[Plan] -->|issue-manager| EPIC[Create Epic]
    EPIC --> BREAK[Break Down]
    BREAK -->|issue-manager| SUBS[Sub-Issues Created & Linked]
    SUBS --> IMPL[Implement]
    IMPL -->|Closes #N| CLOSE[Child Closed]
    CLOSE -->|issue-manager| SYNC[Parent Task List Updated]
    SYNC --> ALL{All Children Done?}
    ALL -->|Yes| EPIC_CLOSE[Close Epic]
    ALL -->|No| IMPL

    style PLAN fill:#f59e0b,color:#fff
    style EPIC fill:#f59e0b,color:#fff
    style BREAK fill:#f59e0b,color:#fff
    style SUBS fill:#f59e0b,color:#fff
    style SYNC fill:#f59e0b,color:#fff
    style EPIC_CLOSE fill:#22c55e,color:#fff
```

#### Issue Hierarchy & Labels

```
Epic (label: epic)
 ├── Feature / Story (label: enhancement)
 │    ├── Sub-task: implementation
 │    ├── Sub-task: tests (label: test)
 │    └── Sub-task: documentation (label: documentation)
 ├── Bug (label: bug)
 └── Tech Debt (label: tech-debt)

Scope labels:   identity | wallet | payment | fraud | notification | audit | reporting | gateway | infra | frontend
Priority labels: priority-critical | priority-high | priority-medium | priority-low
Size labels:    size-small | size-medium | size-large | size-xlarge
```

### Using the git-guardian agent

Ask the `git-guardian` agent to validate your work:

- "Validate my branch name"
- "Check my last 5 commit messages"
- "Review this PR for convention compliance"

### OpenAPI / Swagger Documentation

Aegis follows a **spec-first, YAML-only** OpenAPI 3 workflow. The canonical API contract lives in `specs/<feature>/contracts/` and is never duplicated in controller code.

**Rules:**

- OpenAPI 3 specs MUST be defined in separate YAML files under `specs/<feature>/contracts/`.
- Controllers MUST NOT contain swagger/OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, `@Schema`, etc.). These duplicate the YAML contract and add complexity without gain.
- The `api-design` skill generates the OpenAPI contract. The `service-builder` agent produces controllers with **zero swagger imports**.
- Swagger UI is **only enabled in the `dev` profile**. It MUST NOT be available in staging or production.
- springdoc is **disabled by default** in every service's base `application.yml` (`springdoc.api-docs.enabled: false` + `springdoc.swagger-ui.enabled: false`) and **enabled in `application-dev.yml`**. There is no `SwaggerConfig` Java class — the two legacy ones were removed.
- Spring Security MUST permit Swagger paths (`/swagger-ui/**`, `/v3/api-docs/**`, `/webjars/**`) only when the `dev` profile is active.
- The OpenAPI spec is served at `/v3/api-docs` and Swagger UI at `/swagger-ui.html` (dev profile only).

**When creating a new endpoint, the service-builder agent MUST:**

1. Implement the endpoint according to the YAML contract in `specs/<feature>/contracts/`.
2. Keep controllers free of swagger annotations.
3. Use plain Java records for request/response DTOs.
4. Document new or changed error scenarios in the YAML contract, not in code.

**Access (dev profile only):**

| Resource | URL |
|----------|-----|
| Swagger UI | `http://localhost:8081/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8081/v3/api-docs` |

### Common commands

```bash
# Enable git hooks
git config core.hooksPath .githooks

# Create a feature branch (example for feature 004)
git checkout -b feature/004-add-payment-validation

# Commit with conventional message
git commit -m "feat(payment): add idempotency key to payment creation"

# Push and open a PR
git push -u origin feature/004-add-payment-validation
```

### PR Body Template

```markdown
## Summary
What this PR does and why.

## Changes
- Change 1
- Change 2

## Testing
How this was tested.

## Checklist
- [ ] Follows hexagonal architecture conventions
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated (if applicable)
- [ ] OpenAPI docs updated (if API changed)
- [ ] No secrets or hardcoded credentials
- [ ] `mvn checkstyle:check` passes
- [ ] `mvn test` passes
```
