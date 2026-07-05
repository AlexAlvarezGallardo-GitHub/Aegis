# Implementation Plan: UC-002 User Authentication

**Branch**: `feature/uc-002-user-authentication` | **Date**: 2026-07-03 | **Spec**: `specs/002-user-authentication/spec.md`

## Summary

Implement user authentication for the Aegis platform. The Identity Service exposes `POST /api/v1/auth/login` and `POST /api/v1/auth/refresh` endpoints. Login validates credentials via BCrypt, issues JWT access/refresh tokens, tracks failed attempts with account lockout after 5 failures, and publishes `UserAuthenticated` and `UserAccountLocked` domain events via the transactional outbox pattern.

## Technical Context

**Language/Version**: Java 21 (records, sealed classes, pattern matching)

**Primary Dependencies**: Spring Boot 3, Spring Security, Spring Kafka, JJWT (0.12.x), BCrypt (existing)

**Storage**: PostgreSQL (Identity Service schema — extended `users` table)

**Messaging**: Apache Kafka (topics: `aegis.identity.user-authenticated`, `aegis.identity.user-account-locked`)

**Testing**: JUnit 5, Mockito, Testcontainers (PostgreSQL, Kafka)

**Target Platform**: Docker containers, local development via docker-compose

**Project Type**: Extension of existing Maven microservice (Identity Service)

**Performance Goals**: Login response < 300ms (p95)

**Constraints**: Hexagonal architecture, 80%+ code coverage, 100% domain coverage

**Scale/Scope**: 2 REST endpoints, 2 domain events, account lockout tracking

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal Architecture | PASS | Strict port/adapter separation, no infra types in domain |
| II. Domain Ownership | PASS | Identity owns auth logic, extends users table |
| III. Event-Driven Communication | PASS | Kafka via outbox pattern for auth events |
| IV. Security-First | PASS | BCrypt verification, JWT signing, account lockout, constant-time comparison |
| V. Test-Driven Quality | PASS | Unit + integration tests with Testcontainers |

## Project Structure

### Documentation (this feature)

```text
specs/002-user-authentication/
├── spec.md
├── plan.md
├── data-model.md
├── tasks.md
└── contracts/
    ├── auth-api.yaml
    ├── user-authenticated-event.json
    └── user-account-locked-event.json
```

### Source Code (new/modified files in existing identity service)

```text
aegis-identity-service/
├── pom.xml                                          # [MODIFY] Add JJWT dependency
├── src/main/java/com/aegis/identity/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── User.java                           # [MODIFY] Add failedLoginAttempts, lockedUntil, authenticate()
│   │   │   ├── TokenPair.java                      # [NEW] Value object for access+refresh tokens
│   │   │   └── Credentials.java                    # [NEW] Value object for auth credentials
│   │   ├── event/
│   │   │   ├── UserAuthenticated.java              # [NEW] Domain event record
│   │   │   └── UserAccountLocked.java              # [NEW] Domain event record
│   │   ├── exception/
│   │   │   ├── InvalidCredentialsException.java    # [NEW] Domain exception
│   │   │   ├── AccountLockedException.java         # [NEW] Domain exception
│   │   │   └── AccountSuspendedException.java      # [NEW] Domain exception
│   │   └── port/
│   │       ├── inbound/
│   │       │   └── AuthenticateUserUseCase.java    # [NEW] Inbound port interface
│   │       └── outbound/
│   │           ├── UserRepository.java             # [MODIFY] Add updateFailedAttempts
│   │           └── TokenProvider.java              # [NEW] Outbound port for JWT
│   ├── application/
│   │   ├── service/
│   │   │   └── AuthenticateUserService.java        # [NEW] Use case implementation
│   │   ├── dto/
│   │   │   ├── AuthenticateUserCommand.java        # [NEW] Command DTO
│   │   │   └── AuthenticationResponse.java         # [NEW] Response DTO
│   │   └── mapper/
│   │       └── AuthMapper.java                     # [NEW] Domain-to-DTO mapper
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── UserJpaEntity.java                  # [MODIFY] Add failedLoginAttempts, lockedUntil
│   │   │   ├── UserRepositoryAdapter.java          # [MODIFY] Add updateFailedAttempts
│   │   │   └── FailedLoginJpaEntity.java           # [NEW] Optional: separate table for tracking
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java               # [NEW] JWT generation + validation
│   │   │   └── JwtAuthenticationFilter.java        # [NEW] Spring Security filter
│   │   └── config/
│   │       └── SecurityConfig.java                 # [MODIFY] Add JWT filter, login permit
│   └── web/
│       ├── controller/
│       │   └── AuthController.java                 # [NEW] Login + refresh endpoints
│       ├── dto/
│       │   ├── LoginRequest.java                   # [NEW] Request DTO with validation
│       │   └── RefreshTokenRequest.java            # [NEW] Request DTO with validation
│       └── advice/
│           └── AuthExceptionHandler.java           # [NEW] Exception handler for auth errors
├── src/main/resources/
│   ├── application.yml                             # [MODIFY] Add JWT config
│   └── db/migration/
│       └── V2__add_auth_fields_to_users.sql        # [NEW] Flyway migration
└── src/test/java/com/aegis/identity/
    ├── domain/
    │   ├── model/UserAuthenticationTest.java       # [NEW] Authentication domain logic tests
    │   └── model/TokenPairTest.java                # [NEW] Token value object tests
    ├── application/
    │   └── service/AuthenticateUserServiceTest.java # [NEW] Use case tests
    ├── infrastructure/
    │   └── security/JwtTokenProviderTest.java      # [NEW] JWT provider tests
    └── web/
        └── controller/AuthControllerIT.java        # [NEW] Integration tests
```

## Complexity Tracking

No complexity violations. The implementation follows existing hexagonal architecture patterns from UC-001.
