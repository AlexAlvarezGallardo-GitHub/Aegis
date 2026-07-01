# Aegis Constitution

## Core Principles

### I. Hexagonal Architecture (NON-NEGOTIABLE)

Every microservice MUST follow the hexagonal (ports-and-adapters) architecture with strict layer separation:

- **Domain layer** (`domain/`): Contains entities, value objects, enums, domain events, domain exceptions, and port interfaces (inbound use cases, outbound repository/gateway contracts). This layer MUST NOT depend on infrastructure, web, or any external framework.
- **Application layer** (`application/`): Implements use cases via services, defines DTOs and mappers. Depends on domain ports, never on infrastructure implementations.
- **Infrastructure layer** (`infrastructure/`): Provides adapters for persistence (JPA entities, Spring Data repositories), messaging (Kafka producers/consumers), Spring configuration, and security configuration.
- **Web layer** (`web/`): Exposes REST controllers, exception handlers (`@RestControllerAdvice`), and request filters. Controllers MUST NOT access repositories directly.

Dependency direction is strictly inward: web and infrastructure depend on application, application depends on domain. Domain depends on nothing.

### II. Domain Ownership

Each microservice is an independent bounded context with full data sovereignty:

- Each service owns its database (PostgreSQL); no shared tables between services.
- Each service has its own Maven module, Docker image, and Kubernetes deployment.
- Domain models MUST NOT leak across service boundaries. If another service needs domain data, it receives it through events or API contracts, never through direct database access.
- Service naming: `aegis-<service>-service`. Package naming: `com.aegis.<service>`.

### III. Event-Driven Communication

Inter-service communication MUST follow asynchronous, event-driven patterns:

- Primary channel: Apache Kafka with topic naming `aegis.<service>.<event>` (e.g., `aegis.payment.completed`).
- Event schemas MUST be well-defined, versioned, and documented.
- Synchronous inter-service calls are permitted ONLY through the API Gateway and MUST include circuit breakers.
- Each service publishes domain events when significant state changes occur and consumes events from other services to maintain eventual consistency.

### IV. Security-First

Security is a non-negotiable cross-cutting concern applied at every layer:

- Authentication: OAuth2 + JWT with refresh token rotation.
- Authorization: Role-Based Access Control (RBAC) enforced via `@PreAuthorize` annotations.
- All endpoints require authentication unless explicitly marked `@PermitAll`.
- No secrets in source code. Use environment variables or HashiCorp Vault.
- All security events MUST be logged for audit purposes.
- API responses MUST NOT expose internal implementation details, stack traces, or sensitive data.

### V. Test-Driven Quality

Testing is mandatory at every level with enforced coverage thresholds:

- **Unit tests** (`*Test.java`): JUnit 5 + Mockito. One test class per production class. Arrange-Act-Assert pattern. 100% coverage on domain logic.
- **Integration tests** (`*IT.java`): Testcontainers for PostgreSQL, Kafka, Redis. Real infrastructure interactions.
- **Contract tests**: WireMock for external service dependencies. Consumer-driven contracts.
- Minimum 80% code coverage per service.
- Critical paths (payment processing, fraud detection) require exhaustive edge-case and error-scenario coverage.

## Technology Stack & Infrastructure Constraints

### Mandatory Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 (records, sealed classes, pattern matching) |
| Framework | Spring Boot 3 |
| Persistence | Spring Data JPA + PostgreSQL |
| Messaging | Apache Kafka (spring-kafka) |
| Security | Spring Security + OAuth2 + JWT |
| Build | Maven (multi-module) |
| Frontend | Angular + Angular Material |
| Containerization | Docker (multi-stage build, distroless base image) |
| Orchestration | Kubernetes (namespace per environment) |
| Packaging | Helm (one chart per service with shared values) |
| CI/CD | GitHub Actions (matrix builds, parallel test execution) |

### Constraints

- No shared libraries for domain logic between services (shared utilities only via `aegis-common` module).
- Database tables MUST use `snake_case`. Java classes use `PascalCase`. Methods and variables use `camelCase`.
- REST paths: `/api/v1/<resource>`. URL versioning only.
- Standard error response: `{ "code", "message", "details", "timestamp" }`.
- Pagination via `page`, `size`, `sort` query parameters.
- All endpoints documented with OpenAPI 3 via separate YAML files under `specs/<feature>/contracts/`. Controllers MUST NOT carry OpenAPI/swagger annotations.

## Development Workflow & Quality Gates

### Code Quality Gates

- All public domain port methods MUST have Javadoc.
- `@Validated` on all controller inputs.
- Exception hierarchy per service extending `AegisException`.
- No God classes (> 300 lines), no God methods (> 20 lines preferred).
- No infrastructure types in domain layer.
- DRY, SOLID, and Clean Code principles enforced by code-reviewer agent.

### Review Process

- Every feature MUST pass architecture review (architect agent) before implementation.
- Code review (code-reviewer agent) required before merge.
- Security review (security-reviewer agent) required for authentication, authorization, and data-handling changes.
- All reviews validate hexagonal architecture compliance, DDD boundaries, and naming conventions.

### Commit Conventions

Format: `<type>(<scope>): <description>`

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`, `perf`, `security`

Scopes: `identity`, `wallet`, `payment`, `fraud`, `notification`, `audit`, `reporting`, `gateway`, `infra`, `frontend`

### Specification-Driven Development

Features MUST follow the Specify lifecycle:
1. **Constitution** — establish/update project principles (this document)
2. **Specify** — define feature requirements (user stories, functional requirements, success criteria)
3. **Clarify** — resolve ambiguities before planning
4. **Plan** — generate technical design (data model, contracts, research)
5. **Tasks** — produce actionable, dependency-ordered task breakdown
6. **Issues** — the `issue-manager` agent creates epics, features, and sub-tasks in GitHub, linking them with task lists and dependency references
7. **Analyze** — cross-artifact consistency and quality validation
8. **Checklist** — generate domain-specific quality checklists
9. **Implement** — execute tasks phase-by-phase with validation checkpoints; the `issue-manager` agent syncs parent task lists as sub-issues close
10. **Close** — the `issue-manager` agent verifies all children are resolved, updates epics, and closes completed work

## Governance

This constitution supersedes all other development practices within the Aegis platform.

- **Amendments**: Any principle change requires updating this document with a version bump, rationale, and migration plan for existing services.
- **Versioning**: MAJOR for principle removals or redefinitions, MINOR for new principles or material expansions, PATCH for clarifications.
- **Compliance**: All PR reviews and agent validations MUST verify adherence to these principles. The architect agent enforces Principle I and II. The code-reviewer agent enforces Principle V and code quality gates. The security-reviewer agent enforces Principle IV. The issue-manager agent enforces issue hierarchy integrity, label consistency, dependency tracking, and parent-child synchronization.
- **Complexity justification**: Any deviation from these principles MUST be documented in an Architecture Decision Record (ADR) with explicit rationale.

**Version**: 1.0.0 | **Ratified**: 2026-06-25 | **Last Amended**: 2026-06-25
