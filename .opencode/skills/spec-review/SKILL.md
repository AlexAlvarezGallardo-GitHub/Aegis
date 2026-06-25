---
name: spec-review
description: Use when reviewing a feature specification for Aegis. Bridges speckit.checklist with Aegis quality gates — validates hexagonal architecture compliance, DDD boundaries, API conventions, security requirements, event contracts, and testing standards.
---

# Spec Review (Aegis Bridge)

Generate Aegis-specific quality checklists for a feature specification, validating requirements against the platform's architectural principles and engineering conventions.

## Input

The user provides:
- Feature name or spec path (optional — uses current active feature if omitted)
- Review focus areas (optional — defaults to all Aegis quality gates)

## Execution

### 1. Identify Active Feature

Run the prerequisite check to locate the current feature:

```powershell
.specify/scripts/powershell/check-prerequisites.ps1 -Json
```

Parse `FEATURE_DIR` and load `spec.md`, `plan.md` (if exists), and `tasks.md` (if exists).

If no active feature is found, prompt the user to run `/speckit.specify` first or provide a spec path.

### 2. Generate Aegis Quality Checklists

Create the following checklists in `FEATURE_DIR/checklists/`. Each checklist validates **requirements quality**, not implementation correctness.

---

### Checklist: `hexagonal.md` — Hexagonal Architecture Compliance

Validates that the specification respects hexagonal architecture boundaries.

**Category: Domain Purity**
- [ ] CHK001 — Are domain entities and value objects specified without infrastructure dependencies? [Completeness, Spec §Key Entities]
- [ ] CHK002 — Is the domain model self-contained with no references to JPA, Spring, or framework types? [Clarity, Spec §Key Entities]
- [ ] CHK003 — Are domain events defined with clear payload schemas and publishing triggers? [Completeness, Spec §Functional Requirements]
- [ ] CHK004 — Are domain exceptions specified with a service-specific hierarchy extending `AegisException`? [Gap]

**Category: Port & Adapter Separation**
- [ ] CHK005 — Are inbound ports (use cases) defined as interfaces independent of web/REST concerns? [Clarity, Spec §Functional Requirements]
- [ ] CHK006 — Are outbound ports (repositories, gateways) specified as abstractions without implementation details? [Clarity, Spec §Key Entities]
- [ ] CHK007 — Is the dependency direction explicitly inward (web/infrastructure → application → domain)? [Consistency, Constitution §I]

**Category: Layer Responsibilities**
- [ ] CHK008 — Are application services specified as use-case implementations without business logic? [Clarity, Spec §Functional Requirements]
- [ ] CHK009 — Are DTOs defined as separate from domain models with explicit mapping requirements? [Completeness, Spec §Key Entities]
- [ ] CHK010 — Are infrastructure concerns (persistence, messaging, security) excluded from domain and application layers? [Consistency, Constitution §I]

---

### Checklist: `ddd.md` — Domain-Driven Design Boundaries

Validates bounded context integrity and domain ownership.

**Category: Bounded Context**
- [ ] CHK011 — Is the feature scoped to a single bounded context, or are cross-service interactions explicitly documented? [Clarity, Spec §Overview]
- [ ] CHK012 — Are affected services identified with their package naming (`com.aegis.<service>`)? [Completeness, Spec §Assumptions]
- [ ] CHK013 — Is data ownership clearly assigned to a single service with no shared database tables? [Consistency, Constitution §II]

**Category: Inter-Service Communication**
- [ ] CHK014 — Are Kafka events specified for all state changes that other services need to react to? [Completeness, Spec §Functional Requirements]
- [ ] CHK015 — Are event topics named following `aegis.<service>.<event>` convention? [Consistency, Constitution §III]
- [ ] CHK016 — Are synchronous inter-service calls (if any) documented with circuit breaker requirements? [Gap, Spec §Edge Cases]
- [ ] CHK017 — Is eventual consistency addressed for cross-service data synchronization? [Coverage, Spec §Edge Cases]

**Category: Ubiquitous Language**
- [ ] CHK018 — Are domain terms used consistently across user stories, requirements, and entity definitions? [Consistency, Spec §Terminology]
- [ ] CHK019 — Are ambiguous terms (e.g., "transaction", "account") explicitly defined in the context of the affected service? [Clarity, Spec §Key Entities]

---

### Checklist: `api.md` — API Design & Contract Quality

Validates REST API requirements follow Aegis conventions.

**Category: URL Structure & Versioning**
- [ ] CHK020 — Are all REST endpoints specified with `/api/v1/<resource>` path structure? [Consistency, Constitution §Technology Stack]
- [ ] CHK021 — Are resource names plural for collections and singular for single resources? [Clarity, Spec §Functional Requirements]
- [ ] CHK022 — Is URL versioning (not header versioning) used for API evolution? [Consistency, Constitution §Technology Stack]

**Category: Request/Response Contracts**
- [ ] CHK023 — Are standard HTTP methods (GET, POST, PUT, PATCH, DELETE) mapped to appropriate operations? [Consistency, Spec §Functional Requirements]
- [ ] CHK024 — Are pagination requirements (`page`, `size`, `sort`) specified for list endpoints? [Completeness, Spec §Functional Requirements]
- [ ] CHK025 — Is the standard error response format `{ "code", "message", "details", "timestamp" }` required for all error scenarios? [Consistency, Constitution §Technology Stack]
- [ ] CHK026 — Are HTTP status codes (200, 201, 204, 400, 401, 403, 404, 409, 500) mapped to specific scenarios? [Completeness, Spec §Functional Requirements]

**Category: Documentation & Validation**
- [ ] CHK027 — Is OpenAPI 3 documentation required for all endpoints? [Completeness, Constitution §Technology Stack]
- [ ] CHK028 — Are input validation requirements specified with `@Validated` and validation annotations? [Completeness, Spec §Functional Requirements]
- [ ] CHK029 — Are request/response DTOs defined as Java records with explicit field types? [Clarity, Spec §Key Entities]

---

### Checklist: `security.md` — Security Requirements Quality

Validates security requirements meet Aegis security-first principle.

**Category: Authentication & Authorization**
- [ ] CHK030 — Are OAuth2 + JWT authentication requirements specified for all protected endpoints? [Completeness, Constitution §IV]
- [ ] CHK031 — Are refresh token rotation requirements documented? [Gap, Spec §Functional Requirements]
- [ ] CHK032 — Are RBAC roles and permissions defined with `@PreAuthorize` annotation requirements? [Completeness, Spec §Functional Requirements]
- [ ] CHK033 — Are endpoints explicitly marked `@PermitAll` only where justified (e.g., login, registration)? [Clarity, Spec §Functional Requirements]

**Category: Data Protection**
- [ ] CHK034 — Are sensitive data fields identified with encryption-at-rest requirements? [Completeness, Spec §Key Entities]
- [ ] CHK035 — Are secrets management requirements specified (environment variables or Vault, never in code)? [Consistency, Constitution §IV]
- [ ] CHK036 — Are audit logging requirements defined for security-relevant events (login, permission changes, data access)? [Completeness, Spec §Functional Requirements]

**Category: API Security**
- [ ] CHK037 — Are rate limiting requirements specified for public or high-traffic endpoints? [Gap, Spec §Edge Cases]
- [ ] CHK038 — Are input sanitization requirements defined to prevent injection attacks? [Completeness, Spec §Functional Requirements]
- [ ] CHK039 — Is CORS policy specified for frontend-backend communication? [Gap, Spec §Functional Requirements]
- [ ] CHK040 — Are error response requirements explicit about not exposing stack traces or internal details? [Consistency, Constitution §IV]

---

### Checklist: `events.md` — Event Contract Quality

Validates Kafka event requirements are complete and well-defined.

**Category: Event Schema**
- [ ] CHK041 — Are all domain events specified with topic naming `aegis.<service>.<event>`? [Consistency, Constitution §III]
- [ ] CHK042 — Are event payloads defined with field names, types, and validation rules? [Clarity, Spec §Key Entities]
- [ ] CHK043 — Are event versioning requirements documented for schema evolution? [Gap, Spec §Functional Requirements]
- [ ] CHK044 — Are event ordering requirements specified (e.g., per-partition ordering by entity ID)? [Completeness, Spec §Functional Requirements]

**Category: Producer & Consumer Behavior**
- [ ] CHK045 — Are event publishing triggers mapped to specific domain state changes? [Completeness, Spec §Functional Requirements]
- [ ] CHK046 — Are consumer retry and dead-letter queue requirements specified? [Gap, Spec §Edge Cases]
- [ ] CHK047 — Are idempotency requirements defined for event consumers to handle duplicate deliveries? [Completeness, Spec §Edge Cases]
- [ ] CHK048 — Are event consumer lag and monitoring requirements documented? [Gap, Spec §Non-Functional Requirements]

**Category: Eventual Consistency**
- [ ] CHK049 — Are cross-service data synchronization scenarios addressed via events? [Coverage, Spec §Edge Cases]
- [ ] CHK050 — Are saga or choreography patterns specified for distributed transactions (if applicable)? [Gap, Spec §Functional Requirements]

---

### Checklist: `testing.md` — Testing Requirements Quality

Validates testing requirements meet Aegis coverage and quality standards.

**Category: Unit Testing**
- [ ] CHK051 — Are unit test requirements specified for all domain logic with 100% coverage target? [Completeness, Constitution §V]
- [ ] CHK052 — Is the Arrange-Act-Assert pattern required for test structure? [Consistency, Constitution §V]
- [ ] CHK053 — Are test naming conventions specified (descriptive method names, `@DisplayName`)? [Clarity, Spec §Non-Functional Requirements]
- [ ] CHK054 — Is one test class per production class required (no god test classes)? [Consistency, Constitution §V]

**Category: Integration Testing**
- [ ] CHK055 — Are Testcontainers requirements specified for PostgreSQL, Kafka, and Redis integration tests? [Completeness, Constitution §V]
- [ ] CHK056 — Are real infrastructure interaction scenarios defined (not mocked)? [Clarity, Spec §Functional Requirements]
- [ ] CHK057 — Are HTTP endpoint tests specified with MockMvc or WebTestClient? [Completeness, Spec §Functional Requirements]

**Category: Contract Testing**
- [ ] CHK058 — Are WireMock requirements defined for external service dependencies? [Completeness, Constitution §V]
- [ ] CHK059 — Are consumer-driven contracts specified for inter-service API contracts? [Gap, Spec §Functional Requirements]
- [ ] CHK060 — Are Kafka event contract tests required for producer-consumer agreements? [Gap, Spec §Functional Requirements]

**Category: Coverage & Critical Paths**
- [ ] CHK061 — Is minimum 80% code coverage required per affected service? [Consistency, Constitution §V]
- [ ] CHK062 — Are critical paths (payment, fraud detection) flagged for exhaustive edge-case testing? [Completeness, Spec §Functional Requirements]
- [ ] CHK063 — Are test data builder requirements specified for domain entities? [Gap, Spec §Non-Functional Requirements]

---

### 3. Invoke Checklist Workflow

After generating the Aegis-specific checklists, invoke the Specify checklist command to validate and integrate:

```
/speckit.checklist <review focus areas>
```

The Specify command will:
1. Clarify intent (scope, depth, audience)
2. Load feature context (spec, plan, tasks)
3. Generate or append to checklist files
4. Report completion with item counts

### 4. Post-Checklist Validation

After checklists are generated, review for:

- [ ] All Aegis quality gates are covered (hexagonal, DDD, API, security, events, testing)
- [ ] Checklist items reference spec sections or use `[Gap]` markers
- [ ] Items test **requirements quality**, not implementation correctness
- [ ] Traceability: ≥80% of items include spec section references
- [ ] No duplicate items across checklists

## Aegis-Specific Review Patterns

### Hexagonal Architecture Violations to Flag

```markdown
- [ ] CHK### — Does the spec require domain models to use JPA annotations or Spring stereotypes? [Violation, Constitution §I]
- [ ] CHK### — Are controllers specified to access repositories directly? [Violation, Constitution §I]
- [ ] CHK### — Does the domain layer depend on infrastructure or web concerns? [Violation, Constitution §I]
```

### DDD Boundary Violations to Flag

```markdown
- [ ] CHK### — Does the feature require shared database tables between services? [Violation, Constitution §II]
- [ ] CHK### — Are domain models from one service exposed to another without event/API mediation? [Violation, Constitution §II]
- [ ] CHK### — Is the feature scoped to multiple bounded contexts without explicit cross-service coordination? [Ambiguity, Spec §Overview]
```

### Security Violations to Flag

```markdown
- [ ] CHK### — Are endpoints specified without authentication requirements and no `@PermitAll` justification? [Violation, Constitution §IV]
- [ ] CHK### — Are secrets or credentials mentioned in code or configuration files? [Violation, Constitution §IV]
- [ ] CHK### — Are error responses specified to expose stack traces or internal implementation details? [Violation, Constitution §IV]
```

## Conventions

- Checklist files: `FEATURE_DIR/checklists/<domain>.md`
- Checklist item format: `- [ ] CHK### <requirement quality question> [Quality Dimension, Spec §Section]`
- Quality dimensions: Completeness, Clarity, Consistency, Measurability, Coverage, Gap, Ambiguity, Conflict
- Constitution reference: `.specify/memory/constitution.md`
- Next step after review: `/speckit.analyze` (cross-artifact consistency) or `/speckit.implement` (execute tasks)
