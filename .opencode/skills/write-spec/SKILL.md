---
name: write-spec
description: Use when creating a new feature specification for Aegis. Bridges speckit.specify with Aegis domain context — microservices, hexagonal architecture, Kafka events, and payment platform conventions. Populates spec templates with Aegis-aware defaults and routes to the appropriate Specify workflow.
---

# Write Spec (Aegis Bridge)

Generate a feature specification for the Aegis payment platform, pre-populated with microservice and hexagonal-architecture context.

## Input

The user provides:
- Feature description (natural language)
- Target service(s) (optional — inferred from description if omitted)
- Scope: `full`, `backend-only`, `frontend-only` (default: `full`)

## Execution

### 1. Identify Target Services

Analyze the feature description and map it to Aegis service boundaries:

| Domain Keyword | Service |
|---|---|
| user, identity, auth, login, register, role, permission | `identity` |
| wallet, balance, fund, top-up, transfer | `wallet` |
| payment, transaction, charge, refund, checkout | `payment` |
| fraud, risk, anomaly, detection, scoring | `fraud` |
| notification, email, sms, push, alert | `notification` |
| audit, log, trail, compliance | `audit` |
| report, analytics, dashboard, metric | `reporting` |
| gateway, proxy, routing | `gateway` |
| deploy, k8s, helm, docker, ci/cd | `infra` |
| angular, component, page, ui, frontend | `frontend` |

If the feature spans multiple services, note each as an affected bounded context.

### 2. Pre-populate Spec Context

Before invoking `speckit.specify`, enrich the feature description with Aegis defaults:

**Key Entities** (infer from description):
- Each entity maps to a domain model in `com.aegis.<service>.domain.model`
- Entities have value objects for attributes like `Money`, `AccountId`, `TransactionId`
- State transitions are modeled as domain events in `com.aegis.<service>.domain.event`

**Functional Requirements defaults** (per `.specify/memory/constitution.md` §Constraints and Principles III, IV):
- Apply all API, security, and messaging conventions from the constitution as default requirements

**Success Criteria defaults** (per `.specify/memory/constitution.md` §Quality Gates and Principle V):
- Apply all testing, coverage, and architecture compliance requirements from the constitution as default success criteria

**Edge Cases defaults**:
- Concurrent event processing (idempotency)
- Kafka consumer lag and retry behavior
- Database transaction isolation and consistency
- Circuit breaker behavior for synchronous calls

### 3. Invoke Specify Workflow

After enriching context, invoke the Specify lifecycle:

```
/speckit.specify <enriched feature description>
```

The Specify command will:
1. Create a feature branch (via `speckit.git.feature` hook)
2. Generate `specs/<NNN>-<short-name>/spec.md` from template
3. Fill user stories, functional requirements, success criteria
4. Run specification quality validation
5. Report completion with next-step handoffs

### 4. Post-Specify Enrichment

After `speckit.specify` completes, review the generated spec and ensure:

- [ ] Key entities reference Aegis domain model conventions (value objects, enums)
- [ ] Functional requirements include Kafka event publishing for state changes
- [ ] Success criteria include hexagonal architecture compliance
- [ ] Edge cases cover distributed system concerns (eventual consistency, retries, circuit breakers)
- [ ] Assumptions section documents affected services and their bounded contexts

If any of these are missing, update the spec file directly.

## Aegis-Specific Spec Patterns

### User Story Template for Microservices

```markdown
### User Story N - [Title] (Priority: PX)

As a [user/system/service], I want to [action] so that [value].

**Affected Services**: [list of services]
**Domain Events Published**: [list of events, e.g., `aegis.payment.completed`]
**Events Consumed**: [list of events from other services]
**API Endpoints**: [list of REST endpoints involved]

**Acceptance Scenarios**:
1. **Given** [initial state], **When** [action], **Then** [expected outcome]
   - Event `aegis.<service>.<event>` is published with [payload fields]
   - Downstream service [X] processes the event within [timeframe]
```

### Functional Requirement Patterns

```markdown
- **FR-XXX**: System MUST publish domain event `aegis.<service>.<event>` when [state change]
- **FR-XXX**: System MUST expose REST endpoint `POST /api/v1/<resource>` accepting [DTO fields]
- **FR-XXX**: System MUST validate all inputs using `@Validated` and return standard error response
- **FR-XXX**: System MUST authenticate requests via OAuth2 JWT and enforce RBAC with `@PreAuthorize`
- **FR-XXX**: System MUST persist [entity] in PostgreSQL with `snake_case` table naming
```

### Key Entity Patterns

```markdown
- **[Entity]**: Domain model in `com.aegis.<service>.domain.model`. Attributes include [fields] as value objects. State transitions: [states]. Published events: [events].
- **[Value Object]**: Immutable value object (Java record) representing [concept]. Attributes: [fields].
- **[Domain Event]**: Published to Kafka topic `aegis.<service>.<event>`. Payload: [fields with types].
```

## Conventions

- Feature branch naming: sequential (`NNN-feature-name`) per `.specify/init-options.json`
- Spec directory: `specs/<NNN>-<short-name>/`
- Constitution reference: `.specify/memory/constitution.md`
- Next step after spec: `/speckit.clarify` (resolve ambiguities) or `/speckit.plan` (generate technical design)
