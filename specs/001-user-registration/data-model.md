# Data Model: UC-001 User Registration

**Branch**: `feature/uc-001-user-registration` | **Date**: 2026-06-28

---

## Domain Model

### Aggregate Root: User

```
com.aegis.identity.domain.model.User
├── userId: UserId (Value Object)
├── email: Email (Value Object)
├── passwordHash: PasswordHash (Value Object)
├── firstName: String
├── lastName: String
├── status: UserStatus (Enum)
├── registeredAt: Instant
├── updatedAt: Instant
└── version: Long (optimistic locking)
```

### Value Objects

| Value Object | Type | Description |
|-------------|------|-------------|
| `UserId` | `record UserId(UUID value)` | Wraps UUID v7. Type-safe identifier. |
| `Email` | `record Email(String value)` | Normalized (trimmed, lowercased), validated at construction. |
| `PasswordHash` | `record PasswordHash(String hash)` | BCrypt hash string. Constructed via factory method from plaintext. |

### Enums

| Enum | Values | Description |
|------|--------|-------------|
| `UserStatus` | `PENDING_VERIFICATION`, `ACTIVE`, `LOCKED`, `SUSPENDED` | User account lifecycle states. |

### Domain Events

| Event | Type | Payload |
|-------|------|---------|
| `UserRegistered` | `record` | `eventId: UUID, eventType: String, userId: UUID, email: String, firstName: String, lastName: String, registeredAt: Instant, correlationId: String` |

---

## Persistence Model

### Table: `users`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | `PRIMARY KEY` | User ID (UUID v7) |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` | Normalized email address |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | BCrypt hash |
| `first_name` | `VARCHAR(100)` | `NOT NULL` | User's first name |
| `last_name` | `VARCHAR(100)` | `NOT NULL` | User's last name |
| `status` | `VARCHAR(30)` | `NOT NULL` | User status enum value |
| `registered_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Registration timestamp (UTC) |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Last update timestamp (UTC) |
| `version` | `BIGINT` | `NOT NULL`, `DEFAULT 0` | Optimistic locking version |

**Indexes**:
- `idx_users_email` — unique index on `email` (implicit from UNIQUE constraint)
- `idx_users_status` — index on `status` for administrative queries

### Table: `outbox_events`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | `UUID` | `PRIMARY KEY` | Event ID |
| `aggregate_type` | `VARCHAR(100)` | `NOT NULL` | Aggregate type (e.g., "USER") |
| `aggregate_id` | `UUID` | `NOT NULL` | ID of the source aggregate |
| `event_type` | `VARCHAR(100)` | `NOT NULL` | Event type (e.g., "USER_REGISTERED") |
| `payload` | `JSONB` | `NOT NULL` | Event payload as JSON |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | When the event was created |
| `published_at` | `TIMESTAMP WITH TIME ZONE` | `NULL` | When the event was published to Kafka |
| `status` | `VARCHAR(20)` | `NOT NULL`, `DEFAULT 'PENDING'` | `PENDING` or `PUBLISHED` |

**Indexes**:
- `idx_outbox_status_created` — composite index on `(status, created_at)` for polling unpublished events

---

## JPA Entity Mapping

### UserJpaEntity

```java
@Entity
@Table(name = "users")
public class UserJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
```

### OutboxEventJpaEntity

```java
@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {
    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false, length = 20)
    private String status;
}
```

---

## Domain-to-JPA Mapping

| Domain Concept | JPA Representation |
|---------------|-------------------|
| `UserId.value()` | `UserJpaEntity.id` |
| `Email.value()` | `UserJpaEntity.email` |
| `PasswordHash.hash()` | `UserJpaEntity.passwordHash` |
| `User.firstName` | `UserJpaEntity.firstName` |
| `User.lastName` | `UserJpaEntity.lastName` |
| `User.status` | `UserJpaEntity.status` (enum as STRING) |
| `User.registeredAt` | `UserJpaEntity.registeredAt` |
| `User.version` | `UserJpaEntity.version` |

The `UserRepositoryAdapter` handles bidirectional mapping between `User` (domain aggregate) and `UserJpaEntity` (persistence entity).

---

## DDL (PostgreSQL)

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    registered_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_status ON users (status);

CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at    TIMESTAMP WITH TIME ZONE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);
```

---

## Flyway Migration

Migration file: `V1__create_users_and_outbox_tables.sql`

Location: `aegis-identity-service/src/main/resources/db/migration/`
