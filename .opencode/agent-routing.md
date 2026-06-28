# Agent Routing Guide

This document defines how tasks from `tasks.md` are routed to specialized agents during the `/speckit.implement` phase.

## Routing Strategy

Tasks are routed based on three dimensions:
1. **Phase** (Setup, Foundational, User Stories, Polish)
2. **Keywords** in task description
3. **File paths** referenced in task

## Phase-Based Routing

### Setup Phase (Phase 1)
**Primary Agent**: `service-builder`
**Secondary Agent**: `infra-engineer`

Route to `service-builder`:
- Maven module creation
- Package structure scaffolding
- Spring Boot starter dependencies
- Application configuration files

Route to `infra-engineer`:
- Dockerfile creation
- docker-compose.yml
- Helm chart generation
- Kubernetes manifests
- GitHub Actions workflows

### Foundational Phase (Phase 2)
**Primary Agent**: `service-builder`

Route to `service-builder`:
- Shared infrastructure (exception handlers, base classes)
- Common utilities
- Cross-cutting concerns
- Database migrations (Flyway/Liquibase)

### User Story Phases (Phase 3+)
**Routing by task type within each story**

See "Task-Type Routing" section below.

### Polish Phase (Final Phase)
**Primary Agents**: `test-engineer`, `code-reviewer`, `security-reviewer`

Route to `test-engineer`:
- Integration test suites
- Performance tests
- Test coverage validation

Route to `code-reviewer`:
- Code quality review
- SOLID principles validation
- Clean code enforcement

Route to `security-reviewer`:
- Security audit
- OWASP compliance check
- Secrets scanning

Route to `architect`:
- Architecture validation
- DDD boundary verification
- Hexagonal architecture compliance

## Task-Type Routing (Within Phases)

### Infrastructure Tasks → `infra-engineer`
**Keywords**: Dockerfile, Helm, Kubernetes, K8s, docker-compose, GitHub Actions, CI/CD, deployment, namespace, pod, service mesh

**File patterns**:
- `Dockerfile*`
- `docker-compose*.yml`
- `helm/**`
- `k8s/**`
- `.github/workflows/**`
- `*.yaml` (Kubernetes manifests)

**Example tasks**:
```
- [ ] T001 Create Dockerfile with multi-stage build
- [ ] T002 Generate Helm chart for payment-service
- [ ] T003 Set up GitHub Actions CI/CD pipeline
```

### Domain Model Tasks → `service-builder`
**Keywords**: entity, Entity, model, Model, value object, ValueObject, enum, domain, aggregate, repository, Repository

**File patterns**:
- `domain/model/**`
- `domain/event/**`
- `domain/exception/**`
- `infrastructure/persistence/entity/**`
- `infrastructure/persistence/repository/**`

**Example tasks**:
```
- [ ] T010 [US1] Create Payment entity in domain/model/Payment.java
- [ ] T011 [US1] Define PaymentStatus enum
- [ ] T012 [US1] Create PaymentRepository interface
```

### Application Service Tasks → `service-builder`
**Keywords**: service, Service, use case, UseCase, application, port, inbound, outbound, mapper, Mapper, DTO

**File patterns**:
- `application/service/**`
- `application/mapper/**`
- `application/dto/**`
- `domain/port/**`

**Example tasks**:
```
- [ ] T015 [US1] Implement CreatePaymentUseCase interface
- [ ] T016 [US1] Create PaymentService implementation
- [ ] T017 [US1] Build PaymentMapper for DTO conversion
```

### API/Controller Tasks → `service-builder` (uses `api-design` skill)
**Keywords**: controller, Controller, REST, endpoint, API, @RestController, @RequestMapping, OpenAPI, Swagger

**File patterns**:
- `web/controller/**`
- `web/advice/**`
- `api/**`

**Example tasks**:
```
- [ ] T020 [US1] Create PaymentController with REST endpoints
- [ ] T021 [US1] Implement exception handler for PaymentNotFoundException
- [ ] T022 [US1] Add OpenAPI documentation for payment endpoints
```

**Skill invocation**: When routing API tasks, invoke the `api-design` skill for OpenAPI contract generation.

### Event/Messaging Tasks → `service-builder` (uses `event-design` skill)
**Keywords**: Kafka, event, Event, producer, Producer, consumer, Consumer, topic, message, Message, @KafkaListener

**File patterns**:
- `infrastructure/messaging/**`
- `domain/event/**`

**Example tasks**:
```
- [ ] T025 [US1] Create PaymentCompletedEvent domain event
- [ ] T026 [US1] Implement PaymentEventProducer for Kafka
- [ ] T027 [US1] Build NotificationEventConsumer
```

**Skill invocation**: When routing event tasks, invoke the `event-design` skill for event schema design.

### Frontend Tasks → `frontend-builder`
**Keywords**: Angular, component, Component, template, service (frontend), NgRx, Signal, route, routing, Material, UI, frontend

**File patterns**:
- `aegis-frontend/**`
- `*.component.ts`
- `*.component.html`
- `*.service.ts` (in frontend context)

**Example tasks**:
```
- [ ] T030 [US1] Create PaymentFormComponent
- [ ] T031 [US1] Implement PaymentService for API calls
- [ ] T032 [US1] Add payment routes and navigation
```

### Test Tasks → `test-engineer`
**Keywords**: test, Test, IT.java, @Test, Mockito, Testcontainers, WireMock, coverage, assertion

**File patterns**:
- `src/test/**`
- `*Test.java`
- `*IT.java`

**Example tasks**:
```
- [ ] T035 [US1] Write unit tests for PaymentService
- [ ] T036 [US1] Create integration tests with Testcontainers
- [ ] T037 [US1] Add contract tests for PaymentController
```

### Security Tasks → `security-reviewer` (review only)
**Keywords**: security, Security, OAuth2, JWT, authentication, authorization, @PreAuthorize, secrets, vulnerability

**File patterns**:
- `infrastructure/security/**`
- `SecurityConfig.java`

**Example tasks**:
```
- [ ] T040 Configure OAuth2 resource server
- [ ] T041 Implement JWT token validation
- [ ] T042 Add @PreAuthorize annotations to controllers
```

**Note**: Security tasks are implemented by `service-builder`, then reviewed by `security-reviewer`.

## Post-Implementation Review

After all tasks are completed, trigger a multi-agent review:

### 1. Architecture Review → `architect`
**Validation checklist**:
- Hexagonal architecture compliance (Principle I)
- DDD boundaries respected (Principle II)
- Event-driven communication patterns (Principle III)
- No circular dependencies
- Service decomposition correct

### 2. Code Quality Review → `code-reviewer`
**Validation checklist**:
- SOLID principles followed
- Clean code standards met
- Naming conventions applied
- No code duplication
- Proper exception handling

### 3. Security Review → `security-reviewer`
**Validation checklist**:
- OAuth2/JWT implementation correct
- No hardcoded secrets
- Input validation in place
- SQL injection prevented
- OWASP Top 10 compliance

## Routing Decision Tree

```
Task arrives
    │
    ├─ Phase = Setup?
    │   ├─ Keywords: Dockerfile, Helm, K8s → infra-engineer
    │   └─ Otherwise → service-builder
    │
    ├─ Phase = Foundational?
    │   └─ → service-builder
    │
    ├─ Phase = User Story (US1, US2, etc.)?
    │   ├─ Keywords: test, Test, IT.java → test-engineer
    │   ├─ Keywords: Dockerfile, Helm → infra-engineer
    │   ├─ Keywords: Angular, component → frontend-builder
    │   ├─ Keywords: controller, REST, API → service-builder (api-design skill)
    │   ├─ Keywords: Kafka, event, producer → service-builder (event-design skill)
    │   ├─ Keywords: entity, model, service → service-builder
    │   └─ Keywords: security, OAuth2 → service-builder (then security-reviewer)
    │
    ├─ Phase = Polish?
    │   ├─ Keywords: test, coverage → test-engineer
    │   ├─ Keywords: review, quality → code-reviewer
    │   ├─ Keywords: security, audit → security-reviewer
    │   └─ Keywords: architecture, DDD → architect
    │
    └─ All tasks complete?
        └─ Trigger post-implementation review:
            ├─ architect
            ├─ code-reviewer
            └─ security-reviewer
```

## Agent Invocation Syntax

When routing a task to an agent, use this format:

```
Dispatching task to {agent-name}:

Task: {task-description}
File: {file-path}
Context: {relevant-context-from-plan}

Execute according to your agent instructions and constitution principles.
```

## Skill Invocation

When a task requires a skill (api-design, event-design), invoke it before agent execution:

```
Invoking skill: {skill-name}

Context: {task-requirements}

After skill completes, pass generated artifacts to {agent-name} for implementation.
```

## Error Handling

If an agent fails to complete a task:
1. Log the error with task ID and agent name
2. Halt execution for non-parallel tasks
3. For parallel tasks, continue with successful tasks and report failures
4. Suggest manual intervention or task reassignment

## Progress Reporting

After each task completion:
```
✓ Task {ID} completed by {agent-name}
  File: {file-path}
  Status: {success/partial/failed}
```

After each phase completion:
```
Phase {N} complete:
- Tasks completed: {count}
- Agents used: {agent-list}
- Next phase: {phase-name}
```
