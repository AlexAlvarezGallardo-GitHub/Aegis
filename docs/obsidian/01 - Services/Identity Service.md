---
type: service
service: aegis-identity-service
layer: all
tags: [ddd, hexagonal, java, spring, security]
status: implemented
port: 8081
database: aegis_identity
---

# Identity Service

**Purpose**: User lifecycle management — registration, email/password authentication, lockout after 5 failed attempts, and refresh token rotation.

```mermaid
graph TB
    subgraph "Hexagonal Architecture"
        direction TB
        Web["Web Layer<br/>RegistrationController<br/>AuthController"]
        App["Application Layer<br/>RegisterUserService<br/>AuthenticateUserService<br/>RefreshTokenService"]
        Domain["Domain Layer<br/>User, UserId, Email<br/>UserRegistered, UserAuthenticated"]
        Infra["Infrastructure Layer<br/>UserRepositoryAdapter<br/>KafkaEventPublisher"]
        Web --> App --> Domain
        Domain --> Infra
    end
    Client["Client (BFF/Angular)"] -->|HTTP| Web
    Infra -->|JPA| PG[(PostgreSQL)]
    Infra -->|Outbox| Kafka[("Kafka<br/>(identity events)")]
    Kafka --> Wallet[Wallet Service]
    Kafka --> Audit[Audit Service]
    style Client fill:#bbf,stroke:#333,color:#000
    style Web fill:#bbf,stroke:#333,color:#000
    style App fill:#bbf,stroke:#333,color:#000
    style Domain fill:#bbf,stroke:#333,color:#000
    style Wallet fill:#bbf,stroke:#333,color:#000
    style Audit fill:#bbf,stroke:#333,color:#000
    style Infra fill:#fdb,stroke:#333,color:#000
    style Kafka fill:#fdb,stroke:#333,color:#000
    style PG fill:#afa,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant Client as Client/BFF
    participant Ctrl as RegistrationController
    participant Svc as RegisterUserService
    participant User as User (Domain)
    participant Repo as UserRepository
    participant Event as EventPublisher
    participant DB as PostgreSQL
    participant Kafka as Kafka (Outbox)

    Client->>Ctrl: POST /api/v1/users/register
    Ctrl->>Svc: register(command)
    Svc->>Repo: findByEmail(email)
    alt email already exists
        Repo-->>Svc: found → throw DuplicateEmailException
    end
    Svc->>Svc: hashPassword(password)
    Svc->>User: User.create(email, passwordHash)
    Svc->>Repo: save(user)
    Repo->>DB: INSERT users
    Repo->>DB: INSERT outbox_event (UserRegistered)
    Svc-->>Ctrl: UserRegistrationResponse
    Ctrl-->>Client: 201 Created
    DB-->>Kafka: Outbox relay
```

## Hexagonal Structure

### Domain (`com.aegis.identity.domain`)
- **Models**: [[02 - Domain Models/User\|User]], [[02 - Domain Models/UserId\|UserId]], [[02 - Domain Models/Email\|Email]], `Password`, [[02 - Domain Models/PasswordHash\|PasswordHash]], [[02 - Domain Models/UserStatus\|UserStatus]], [[02 - Domain Models/Credentials\|Credentials]], [[02 - Domain Models/TokenPair\|TokenPair]]
- **Events**: [[03 - Domain Events/UserRegistered\|UserRegistered]], [[03 - Domain Events/UserAuthenticated\|UserAuthenticated]], [[03 - Domain Events/UserAccountLocked\|UserAccountLocked]]
- **Exceptions**: `DuplicateEmailException`, `InvalidEmailException`, `WeakPasswordException`, `InvalidRegistrationException`, `InvalidCredentialsException`, `AccountLockedException`, `AccountSuspendedException`
- **Inbound Ports**: [[04 - Ports/inbound/RegisterUserUseCase\|RegisterUserUseCase]], [[04 - Ports/inbound/AuthenticateUserUseCase\|AuthenticateUserUseCase]], `RefreshTokenUseCase`
- **Outbound Ports**: [[04 - Ports/outbound/UserRepository\|UserRepository]], [[04 - Ports/outbound/PasswordHasher\|PasswordHasher]], [[04 - Ports/outbound/TokenProvider\|TokenProvider]], [[04 - Ports/outbound/EventPublisher\|EventPublisher]], `RefreshTokenRepository`

### Application (`com.aegis.identity.application`)
- **Services**: `RegisterUserService`, `AuthenticateUserService`, `RefreshTokenService`
- **DTOs**: `RegisterUserCommand`, `UserRegistrationResponse`, `AuthenticateUserCommand`, `AuthenticationResponse`

### Infrastructure (`com.aegis.identity.infrastructure`)
- **Persistence**: `UserJpaEntity`, `UserJpaRepository`, `UserRepositoryAdapter`, `OutboxEventJpaEntity`, `OutboxEventJpaRepository`, `OutboxRelayScheduler`
- **Security**: `BCryptPasswordHasher`, `JwtTokenProvider`, `JwtAuthenticationFilter`
- **Messaging**: `KafkaEventPublisher`
- **Config**: `SecurityConfig`, `KafkaConfig`

### Web (`com.aegis.identity.web`)
- **Controllers**: `RegistrationController`, `AuthController`
- **DTOs**: `RegisterUserRequest`, `LoginRequest`, `RefreshTokenRequest`
- **Advice**: `RegistrationExceptionHandler`, `AuthExceptionHandler`

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/users/register` | Register new user (201) |
| POST | `/api/v1/auth/login` | Login (access token; `refreshToken` is NULL) |
| POST | `/api/v1/auth/refresh` | Rotate refresh tokens |

## Business Rules

1. Account locks after 5 consecutive failed authentication attempts (`UserStatus.LOCKED`)
2. `UserAuthenticated` is emitted on both success and failure, carrying a `success` flag
3. Refresh tokens are rotated on every `/auth/refresh` call

## Domain Events Produced

| Event | Topic |
|-------|-------|
| [[03 - Domain Events/UserRegistered\|UserRegistered]] | `aegis.identity.user-registered` |
| [[03 - Domain Events/UserAuthenticated\|UserAuthenticated]] | `aegis.identity.user-authenticated` |
| [[03 - Domain Events/UserAccountLocked\|UserAccountLocked]] | `aegis.identity.user-account-locked` |

## Dependencies

- **Depends on**: [[01 - Services/Common Module\|Common Module]], PostgreSQL, Kafka
- **Depended by**: [[01 - Services/BFF Service\|BFF Service]] (proxies auth)
- **Consumed by**: [[01 - Services/Frontend\|Frontend]] via BFF

## Flyway Migrations

| File | Description |
|------|-------------|
| `V1__create_users_and_outbox_tables.sql` | Initial schema |
| `V2__add_auth_fields_to_users.sql` | Failed attempts + lockout |
| `V3__create_refresh_tokens_table.sql` | Refresh token storage |
