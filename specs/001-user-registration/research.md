# Research: UC-001 User Registration

**Branch**: `feature/uc-001-user-registration` | **Date**: 2026-06-28

---

## 1. Password Hashing Strategy

### Decision: BCrypt with strength factor 12

**Rationale**: BCrypt is the industry standard for password hashing in Java/Spring ecosystems. It includes a built-in salt, is deliberately slow (resistant to brute-force), and is supported natively by Spring Security's `BCryptPasswordEncoder`.

**Alternatives considered**:
- **Argon2id**: More modern, winner of the Password Hashing Competition. However, Spring Security support requires additional dependencies and configuration. BCrypt is sufficient for a portfolio project and has broader ecosystem support.
- **SCrypt**: Good alternative but less commonly used in Spring Boot projects.
- **PBKDF2**: NIST-approved but lacks built-in salt management and is less resistant to GPU attacks.

**Strength factor**: 12 (approximately 250ms per hash on modern hardware). This balances security with user experience for registration.

**Implementation**: Use Spring Security's `BCryptPasswordEncoder` with `strength=12`. Wrap in a domain service `PasswordHashingService` in the application layer to maintain hexagonal architecture boundaries.

---

## 2. Transactional Outbox Pattern

### Decision: Polling Publisher with dedicated `outbox_events` table

**Rationale**: The transactional outbox pattern guarantees that domain events are published even if Kafka is temporarily unavailable. The event is written to an outbox table within the same database transaction as the business operation, then asynchronously forwarded to Kafka.

**Alternatives considered**:
- **CDC (Change Data Capture) with Debezium**: More robust for production but adds operational complexity (Debezium Connect deployment). Overkill for a portfolio project.
- **Kafka transactions**: Spring Kafka supports producer transactions, but they require `transactionIdPrefix` configuration and have limitations with consumer groups. The outbox pattern is simpler and more portable.
- **Dual-write without outbox**: Risk of data inconsistency if the application crashes between DB write and Kafka publish.

**Implementation**:
- `outbox_events` table with columns: `id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload` (JSON), `created_at`, `published_at`, `status`
- A scheduled polling task (Spring `@Scheduled` or `@Async`) reads unpublished events and forwards them to Kafka
- On successful publish, update `status` to `PUBLISHED` and set `published_at`
- Events older than 7 days with `PUBLISHED` status are archived/deleted

---

## 3. UUID v7 for User IDs

### Decision: UUID v7 (time-ordered) for UserId

**Rationale**: UUID v7 combines the uniqueness guarantees of random UUIDs with time-ordering, making them suitable for database primary keys (better B-tree index performance than UUID v4). Java 21 does not include native UUID v7 support, so a utility class is needed.

**Alternatives considered**:
- **UUID v4**: Random, widely supported, but poor B-tree index performance due to random distribution.
- **Snowflake IDs**: Excellent performance but require a distributed ID generator service.
- **Database sequences**: Simple but couples the domain model to the persistence layer.

**Implementation**: Create a `UuidV7Generator` utility class in `aegis-common` module. UUID v7 encodes a Unix timestamp in the most significant 48 bits, providing natural ordering.

---

## 4. Email Validation Strategy

### Decision: Jakarta Bean Validation + custom domain validator

**Rationale**: Two-layer validation ensures both syntactic correctness and domain-level invariants.

**Layer 1 (Infrastructure/Web)**: Jakarta `@Email` annotation on the DTO for basic format validation at the controller level.

**Layer 2 (Domain)**: `Email` value object constructor performs RFC 5322 validation using a compiled regex pattern. This ensures that no invalid `Email` instance can exist in the domain model.

**Normalization**: Email is trimmed and lowercased before validation. The normalized form is what gets persisted and compared for uniqueness.

**Regex**: Use the standard `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$` pattern for practical email validation (stricter than RFC 5322 but covers 99.9% of real-world emails).

---

## 5. Kafka Topic Configuration

### Decision: Single partition, replication factor 3, retention 7 days

**Topic**: `aegis.identity.user-registered`

| Property | Value | Rationale |
|----------|-------|-----------|
| Partitions | 1 | User registration volume is low; single partition ensures ordering per user |
| Replication factor | 3 | Standard for fault tolerance in a 3-broker cluster |
| Retention | 7 days | Sufficient for consumers to recover from extended downtime |
| Cleanup policy | delete | No need for compaction on event topics |
| Key serializer | StringSerializer | Event ID as key for partition affinity |
| Value serializer | JsonSerializer | JSON payload for human readability and schema evolution |

**Schema evolution**: Use a `schemaVersion` field in the event payload. Consumers must handle version negotiation. For v1, `schemaVersion: "1.0"`.

---

## 6. Hexagonal Architecture Boundaries

### Decision: Strict port/adapter separation with domain purity

**Domain layer** (`com.aegis.identity.domain`):
- `User` aggregate root with factory method `User.register(email, password, firstName, lastName)`
- `Email`, `PasswordHash`, `UserId` value objects (Java records)
- `UserStatus` enum
- `UserRegistered` domain event (Java record)
- `UserRepository` port interface (outbound)
- `RegisterUserUseCase` port interface (inbound)
- `PasswordPolicy` domain service for validation rules
- `DomainException` hierarchy: `DuplicateEmailException`, `InvalidEmailException`, `WeakPasswordException`

**Application layer** (`com.aegis.identity.application`):
- `RegisterUserService` implements `RegisterUserUseCase`
- `RegisterUserCommand` DTO (input)
- `UserRegistrationResponse` DTO (output)
- `UserMapper` for DTO <-> Domain conversion
- `EventPublisher` port interface (outbound) for publishing domain events

**Infrastructure layer** (`com.aegis.identity.infrastructure`):
- `UserJpaEntity` (persistence entity, separate from domain `User`)
- `UserJpaRepository` (Spring Data JPA)
- `UserRepositoryAdapter` implements domain `UserRepository` port
- `KafkaEventPublisher` implements application `EventPublisher` port
- `OutboxEventJpaEntity` and `OutboxEventJpaRepository`
- `OutboxRelayScheduler` for polling and forwarding events to Kafka
- `BCryptPasswordHasher` implements domain password hashing interface
- `SecurityConfig` (Spring Security configuration)

**Web layer** (`com.aegis.identity.web`):
- `RegistrationController` (`@RestController`)
- `RegisterUserRequest` DTO (JSON request body)
- `RegistrationExceptionHandler` (`@RestControllerAdvice`)

---

## 7. Error Handling Strategy

### Decision: Domain exceptions mapped to HTTP responses via exception handler

**Domain exceptions** (in `com.aegis.identity.domain.exception`):
- `DuplicateEmailException` -> `409 Conflict`
- `InvalidEmailException` -> `400 Bad Request`
- `WeakPasswordException` -> `400 Bad Request`
- `InvalidRegistrationException` -> `400 Bad Request`

All extend `AegisException` (from `aegis-common`) which provides `code`, `message`, `details`, and `timestamp`.

**Exception handler** (`RegistrationExceptionHandler`):
- Catches domain exceptions and maps to `ErrorResponse` DTO
- Catches `MethodArgumentNotValidException` (Jakarta validation failures) and maps to `ErrorResponse`
- Catches `ConstraintViolationException` and maps to `ErrorResponse`
- Default fallback for unexpected exceptions returns `500 Internal Server Error` with generic message (no stack trace)

---

## 8. Testing Strategy

### Decision: Layered testing with Testcontainers for integration tests

**Unit tests** (JUnit 5 + Mockito):
- `UserTest`: Aggregate root behavior, state transitions, factory method
- `EmailTest`: Value object validation, normalization
- `PasswordPolicyTest`: All password rules (valid and invalid cases)
- `RegisterUserServiceTest`: Use case logic with mocked ports

**Integration tests** (Testcontainers):
- `RegistrationControllerIT`: Full HTTP flow with real PostgreSQL and Kafka
- `UserRepositoryAdapterIT`: Persistence adapter with real database
- `OutboxRelayIT`: Outbox pattern end-to-end with real Kafka

**Contract tests** (WireMock):
- Not applicable for UC-001 (no synchronous inter-service calls)

**Coverage target**: 100% on domain layer, 80%+ overall per service
