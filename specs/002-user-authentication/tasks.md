# Tasks: UC-002 User Authentication

**Input**: Design documents from `specs/002-user-authentication/`

**Prerequisites**: plan.md, spec.md, data-model.md, contracts/

---

## Phase 1: Domain Layer (User Story 1 - P1 MVP)

**Purpose**: Core authentication domain model with zero infrastructure dependencies

- [ ] T001 [P] Create `Credentials` value object (record) in `domain/model/`
- [ ] T002 [P] Create `TokenPair` value object (record) in `domain/model/`
- [ ] T003 [P] Create `UserAuthenticated` domain event (record) in `domain/event/`
- [ ] T004 [P] Create `UserAccountLocked` domain event (record) in `domain/event/`
- [ ] T005 [P] Create domain exceptions: `InvalidCredentialsException`, `AccountLockedException`, `AccountSuspendedException` in `domain/exception/`
- [ ] T006 [P] Create `TokenProvider` outbound port (interface) in `domain/port/outbound/`
- [ ] T007 Create `AuthenticateUserUseCase` inbound port (interface) in `domain/port/inbound/`
- [ ] T008 [P] Extend `User` aggregate: add `failedLoginAttempts`, `lockedUntil`, `authenticate()` method, `recordFailedAttempt()`, `resetFailedAttempts()`, `lockAccount()` methods
- [ ] T009 Extend `UserRepository` port: add `saveAndFlush()` or optimistic locking support

---

## Phase 2: Application Layer (User Story 1 - P1 MVP)

**Purpose**: Use case implementation, DTOs, and mappers

- [ ] T010 [P] Create `AuthenticateUserCommand` DTO (record) in `application/dto/`
- [ ] T011 [P] Create `AuthenticationResponse` DTO (record) in `application/dto/`
- [ ] T012 Create `AuthMapper` in `application/mapper/`
- [ ] T013 Implement `AuthenticateUserService` (use case implementation) in `application/service/`

---

## Phase 3: Infrastructure Layer (User Story 1 - P1 MVP)

**Purpose**: JWT provider, persistence, security filter, event publishing

- [ ] T014 [P] Add JJWT dependency to `pom.xml`
- [ ] T015 [P] Create `JwtTokenProvider` implementing `TokenProvider` in `infrastructure/security/`
- [ ] T016 Create `JwtAuthenticationFilter` (Spring Security OncePerRequestFilter) in `infrastructure/security/`
- [ ] T017 [P] Extend `UserJpaEntity`: add `failedLoginAttempts`, `lockedUntil` fields
- [ ] T018 [P] Extend `UserRepositoryAdapter`: implement updated domain methods
- [ ] T019 Update `SecurityConfig`: add JWT filter, permit auth endpoints, configure AuthenticationManager
- [ ] T020 Create Flyway migration `V2__add_auth_fields_to_users.sql`

---

## Phase 4: Web Layer (User Story 1 - P1 MVP)

**Purpose**: REST controller and exception handling

- [ ] T021 [P] Create `LoginRequest` DTO with Jakarta validation in `web/dto/`
- [ ] T022 [P] Create `RefreshTokenRequest` DTO with Jakarta validation in `web/dto/`
- [ ] T023 Create `AuthController` in `web/controller/` (POST /api/v1/auth/login + POST /api/v1/auth/refresh)
- [ ] T024 Create `AuthExceptionHandler` in `web/advice/`

---

## Phase 5: Event Publishing (User Story 1 - P1 MVP)

**Purpose**: Authentication events via transactional outbox

- [ ] T025 Publish `UserAuthenticated` event via existing `EventPublisher` in `AuthenticateUserService`
- [ ] T026 Publish `UserAccountLocked` event via existing `EventPublisher` in `AuthenticateUserService`

---

## Phase 6: Unit Tests (User Story 1 - P1 MVP)

**Purpose**: 100% domain coverage, 80%+ overall

- [ ] T027 [P] Write `UserAuthenticationTest` — authenticate flow, failed attempts, account lockout
- [ ] T028 [P] Write `TokenProviderTest` — JWT generation, validation, expiry
- [ ] T029 Write `AuthenticateUserServiceTest` — use case with mocked ports (happy path + all error paths)

---

## Phase 7: Integration Tests (User Story 1 - P1 MVP)

**Purpose**: End-to-end flow with real infrastructure

- [ ] T030 Write `AuthControllerIT` — full HTTP flow with Testcontainers (PostgreSQL + Kafka)

---

## Phase 8: Configuration & Polish (User Story 1 - P1 MVP)

**Purpose**: Final configuration and validation

- [ ] T031 Update `application.yml` with JWT settings (secret, expiry)
- [ ] T032 Run full build: `mvn clean install` (all tests pass)
- [ ] T033 Run checkstyle: `mvn checkstyle:check`

---

## User Story 2 - Token Refresh (Priority: P2)

- [ ] T034 Implement refresh token validation in `JwtTokenProvider`
- [ ] T035 Add refresh endpoint logic in `AuthenticateUserService`
- [ ] T036 Complete `AuthController` refresh endpoint
- [ ] T037 Write refresh token tests

---

## User Story 3 - Failed Attempt Tracking (Priority: P2)

- [ ] T038 Verify account lockout threshold (5 attempts) in `AuthenticateUserService`
- [ ] T039 Write edge case tests for concurrent failed attempts (optimistic locking)

---

## Dependencies & Execution Order

```
Phase 1 (Domain) → Phase 2 (Application) → Phase 3 (Infrastructure) → Phase 4 (Web)
                                                                           ↓
                                                                    Phase 5 (Events)
                                                                           ↓
                                                                    Phase 6 (Unit Tests)
                                                                           ↓
                                                                 Phase 7 (Integration Tests)
                                                                           ↓
                                                                     Phase 8 (Polish)
```

### Parallel Opportunities

- T001-T006 (value objects, events, exceptions, ports) — all independent
- T010-T011 (DTOs) — independent
- T014-T015 (pom.xml + JWT provider) — independent
- T017 (JPA entity) — independent
- T021-T022 (request DTOs) — independent
- T027-T028 (unit tests) — independent

### Within Each Phase

- Models before services
- Services before endpoints
- Domain before infrastructure
- Infrastructure before web
- Tests after implementation
