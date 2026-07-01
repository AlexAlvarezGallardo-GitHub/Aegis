# Implementation Plan: UC-001 User Registration

**Branch**: `feature/uc-001-user-registration` | **Date**: 2026-06-28 | **Spec**: `specs/001-user-registration/spec.md`

## Summary

Implement user self-registration for the Aegis platform. The Identity Service exposes a `POST /api/v1/users/register` endpoint that validates input, creates a user account in `PENDING_VERIFICATION` status, and publishes a `UserRegistered` domain event via the transactional outbox pattern. Downstream services (Audit, Notification) consume the event asynchronously. An Angular frontend provides the registration form. Docker Compose provisions the full local infrastructure stack.

## Technical Context

**Language/Version**: Java 21 (records, sealed classes, pattern matching)

**Primary Dependencies**: Spring Boot 3, Spring Data JPA, Spring Security, Spring Kafka, Flyway, BCrypt

**Storage**: PostgreSQL (Identity Service schema)

**Messaging**: Apache Kafka (topic: `aegis.identity.user-registered`)

**Testing**: JUnit 5, Mockito, Testcontainers (PostgreSQL, Kafka)

**Frontend**: Angular 18+ with Angular Material

**Target Platform**: Docker containers, local development via docker-compose

**Project Type**: Multi-module Maven project (microservice) + Angular SPA

**Performance Goals**: Registration response < 500ms (p95)

**Constraints**: Hexagonal architecture, 80%+ code coverage, 100% domain coverage

**Scale/Scope**: Single microservice (Identity), 1 REST endpoint, 1 domain event, 1 Angular page

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal Architecture | PASS | Strict port/adapter separation, no infra types in domain |
| II. Domain Ownership | PASS | Identity owns its DB, no shared tables |
| III. Event-Driven Communication | PASS | Kafka via outbox pattern, no sync inter-service calls |
| IV. Security-First | PASS | BCrypt hashing, @PermitAll on registration, no secrets in code |
| V. Test-Driven Quality | PASS | Unit + integration tests with Testcontainers |

## Project Structure

### Documentation (this feature)

```text
specs/001-user-registration/
├── spec.md
├── research.md
├── data-model.md
├── plan.md
├── tasks.md
└── contracts/
    ├── registration-api.yaml
    └── user-registered-event.json
```

### Source Code (repository root)

```text
aegis-common/
└── src/main/java/com/aegis/common/
    ├── domain/
    │   └── exception/AegisException.java
    └── util/UuidV7Generator.java

aegis-identity-service/
├── pom.xml
├── src/main/java/com/aegis/identity/
│   ├── IdentityServiceApplication.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── UserId.java
│   │   │   ├── Email.java
│   │   │   ├── PasswordHash.java
│   │   │   └── UserStatus.java
│   │   ├── event/
│   │   │   └── UserRegistered.java
│   │   ├── exception/
│   │   │   ├── DuplicateEmailException.java
│   │   │   ├── InvalidEmailException.java
│   │   │   ├── WeakPasswordException.java
│   │   │   └── InvalidRegistrationException.java
│   │   └── port/
│   │       ├── inbound/
│   │       │   └── RegisterUserUseCase.java
│   │       └── outbound/
│   │           ├── UserRepository.java
│   │           ├── EventPublisher.java
│   │           └── PasswordHasher.java
│   ├── application/
│   │   ├── service/
│   │   │   └── RegisterUserService.java
│   │   ├── dto/
│   │   │   ├── RegisterUserCommand.java
│   │   │   └── UserRegistrationResponse.java
│   │   └── mapper/
│   │       └── UserMapper.java
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── UserJpaEntity.java
│   │   │   ├── UserJpaRepository.java
│   │   │   ├── UserRepositoryAdapter.java
│   │   │   ├── OutboxEventJpaEntity.java
│   │   │   ├── OutboxEventJpaRepository.java
│   │   │   └── OutboxRelayScheduler.java
│   │   ├── messaging/
│   │   │   └── KafkaEventPublisher.java
│   │   ├── config/
│   │   │   ├── KafkaConfig.java
│   │   │   └── SecurityConfig.java
│   │   └── security/
│   │       └── BCryptPasswordHasher.java
│   └── web/
│       ├── controller/
│       │   └── RegistrationController.java
│       ├── dto/
│       │   └── RegisterUserRequest.java
│       └── advice/
│           └── RegistrationExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__create_users_and_outbox_tables.sql
└── src/test/java/com/aegis/identity/
    ├── domain/
    │   ├── model/UserTest.java
    │   ├── model/EmailTest.java
    │   └── port/outbound/PasswordPolicyTest.java
    ├── application/
    │   └── service/RegisterUserServiceTest.java
    └── infrastructure/
        └── RegistrationControllerIT.java

aegis-frontend/
├── package.json
├── angular.json
├── src/app/
│   ├── features/
│   │   └── registration/
│   │       ├── registration.component.ts
│   │       ├── registration.component.html
│   │       ├── registration.component.scss
│   │       ├── registration.component.spec.ts
│   │       └── registration.service.ts
│   ├── shared/
│   │   └── models/
│   │       └── registration.model.ts
│   └── app.config.ts
└── src/environments/
    ├── environment.ts
    └── environment.prod.ts

docker/
└── docker-compose.yml
```

**Structure Decision**: Multi-module Maven project with a shared `aegis-common` module, standalone `aegis-identity-service`, separate `aegis-frontend` Angular project, and a `docker/` directory for infrastructure compose files.
