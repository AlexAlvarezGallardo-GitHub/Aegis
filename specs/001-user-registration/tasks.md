# Tasks: UC-001 User Registration

**Input**: Design documents from `specs/001-user-registration/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

---

## Phase 1: Infrastructure Setup

**Purpose**: Local development environment with all required services

- [ ] T001 Create `docker/docker-compose.yml` with PostgreSQL, DbGate, Kafka, Zookeeper, and Kafka UI
- [ ] T002 Verify docker-compose starts all services successfully

---

## Phase 2: Project Scaffolding

**Purpose**: Maven multi-module project structure with dependencies

- [ ] T003 Create root `pom.xml` (parent POM with module declarations, dependency management, plugin management)
- [ ] T004 Create `aegis-common` module with `pom.xml` and base package structure
- [ ] T005 Create `aegis-identity-service` module with `pom.xml` and hexagonal package structure
- [ ] T006 Implement `AegisException` base class and `UuidV7Generator` in `aegis-common`
- [ ] T007 Create `application.yml` for Identity Service (datasource, Kafka, server config)
- [ ] T008 Create Flyway migration `V1__create_users_and_outbox_tables.sql`
- [ ] T009 Create `IdentityServiceApplication.java` main class

---

## Phase 3: Domain Layer (User Story 1 - P1 MVP)

**Purpose**: Core domain model with zero infrastructure dependencies

- [ ] T010 [P] Create `UserId` value object (record) in `domain/model/`
- [ ] T011 [P] Create `Email` value object (record) with validation and normalization in `domain/model/`
- [ ] T012 [P] Create `PasswordHash` value object (record) in `domain/model/`
- [ ] T013 [P] Create `UserStatus` enum in `domain/model/`
- [ ] T014 Create `User` aggregate root with `register()` factory method in `domain/model/`
- [ ] T015 [P] Create `UserRegistered` domain event (record) in `domain/event/`
- [ ] T016 [P] Create domain exceptions: `DuplicateEmailException`, `InvalidEmailException`, `WeakPasswordException`, `InvalidRegistrationException` in `domain/exception/`
- [ ] T017 [P] Create port interfaces: `UserRepository`, `EventPublisher`, `PasswordHasher` in `domain/port/`
- [ ] T018 Create `RegisterUserUseCase` inbound port interface in `domain/port/inbound/`

---

## Phase 4: Application Layer (User Story 1 - P1 MVP)

**Purpose**: Use case implementation, DTOs, and mappers

- [ ] T019 [P] Create `RegisterUserCommand` DTO (record) in `application/dto/`
- [ ] T020 [P] Create `UserRegistrationResponse` DTO (record) in `application/dto/`
- [ ] T021 Create `UserMapper` in `application/mapper/`
- [ ] T022 Implement `RegisterUserService` (use case implementation) in `application/service/`

---

## Phase 5: Infrastructure Layer (User Story 1 - P1 MVP)

**Purpose**: Persistence adapters, messaging, security configuration

- [ ] T023 [P] Create `UserJpaEntity` in `infrastructure/persistence/`
- [ ] T024 [P] Create `UserJpaRepository` (Spring Data) in `infrastructure/persistence/`
- [ ] T025 Create `UserRepositoryAdapter` implementing domain port in `infrastructure/persistence/`
- [ ] T026 [P] Create `OutboxEventJpaEntity` in `infrastructure/persistence/`
- [ ] T027 [P] Create `OutboxEventJpaRepository` in `infrastructure/persistence/`
- [ ] T028 Create `OutboxRelayScheduler` for polling and publishing events to Kafka in `infrastructure/persistence/`
- [ ] T029 Create `BCryptPasswordHasher` implementing domain port in `infrastructure/security/`
- [ ] T030 Create `KafkaEventPublisher` implementing application port in `infrastructure/messaging/`
- [ ] T031 Create `KafkaConfig` in `infrastructure/config/`
- [ ] T032 Create `SecurityConfig` with @PermitAll on registration endpoint in `infrastructure/config/`

---

## Phase 6: Web Layer (User Story 1 - P1 MVP)

**Purpose**: REST controller and exception handling

- [ ] T033 Create `RegisterUserRequest` DTO with Jakarta validation in `web/dto/`
- [ ] T034 Create `RegistrationController` in `web/controller/`
- [ ] T035 Create `RegistrationExceptionHandler` in `web/advice/`

---

## Phase 7: Unit Tests (User Story 1 - P1 MVP)

**Purpose**: 100% domain coverage, 80%+ overall

- [ ] T036 [P] Write `EmailTest` — validation, normalization, edge cases
- [ ] T037 [P] Write `UserTest` — aggregate root, factory method, state transitions
- [ ] T038 [P] Write `PasswordPolicyTest` — all password rules
- [ ] T039 Write `RegisterUserServiceTest` — use case with mocked ports (happy path + all error paths)

---

## Phase 8: Integration Tests (User Story 1 - P1 MVP)

**Purpose**: End-to-end flow with real infrastructure

- [ ] T040 Write `RegistrationControllerIT` — full HTTP flow with Testcontainers (PostgreSQL + Kafka)

---

## Phase 9: Frontend (User Story 1 - P1 MVP)

**Purpose**: Angular registration page

- [ ] T041 Scaffold Angular project with Angular Material
- [ ] T042 [P] Create registration models/interfaces in `shared/models/`
- [ ] T043 Create `RegistrationService` (HTTP client) in `features/registration/`
- [ ] T044 Create `RegistrationComponent` (reactive form, validation, error handling) in `features/registration/`
- [ ] T045 Configure environment files and proxy for local API
- [ ] T046 Write `RegistrationComponent` unit test

---

## Phase 10: Polish & Cross-Cutting

**Purpose**: Final validation and cleanup

- [ ] T047 Run full build: `mvn clean install` (all tests pass)
- [ ] T048 Run frontend build: `npm run build`
- [ ] T049 Run frontend lint: `npm run lint`
- [ ] T050 Run checkstyle: `mvn checkstyle:check`
- [ ] T051 End-to-end manual verification: register user via Angular UI, verify DB record, Kafka event, and Kafka UI visibility

---

## Dependencies & Execution Order

```
Phase 1 (Infra) → Phase 2 (Scaffold) → Phase 3 (Domain)
                                           ↓
                                        Phase 4 (Application)
                                           ↓
                                        Phase 5 (Infrastructure)
                                           ↓
                                        Phase 6 (Web)
                                           ↓
                                     Phase 7 (Unit Tests)
                                           ↓
                                  Phase 8 (Integration Tests)
                                           ↓
                                        Phase 9 (Frontend)
                                           ↓
                                        Phase 10 (Polish)
```

### Parallel Opportunities

- T010-T013, T015-T017 (value objects, events, exceptions, ports) — all independent
- T019-T020 (DTOs) — independent
- T023-T024, T026-T027 (JPA entities/repos) — independent
- T036-T038 (unit tests) — independent
- T042 (frontend models) — independent of component
- T043-T044 (service + component) — sequential

### Within Each Phase

- Models before services
- Services before endpoints
- Domain before infrastructure
- Infrastructure before web
- Tests written after implementation (test-after for this iteration; TDD for future features)
