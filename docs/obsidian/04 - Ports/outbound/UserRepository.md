---
type: port
service: aegis-identity-service
layer: domain
tags: [port, outbound, repository]
status: implemented
port-type: outbound
---

# UserRepository

Outbound port for user persistence.

## Methods

| Method | Description |
|--------|-------------|
| `save(user)` → `User` | Persist new or updated user |
| `saveAndFlush(user)` → `User` | Persist and flush the persistence context |
| `findByEmail(email)` → `Optional<User>` | Lookup user by [[02 - Domain Models/Email\|Email]] |
| `findById(userId)` → `Optional<User>` | Lookup by [[02 - Domain Models/UserId\|UserId]] |
| `existsByEmail(email)` → `boolean` | Check email uniqueness |

## Implementation

- **Adapter**: `UserRepositoryAdapter` in `infrastructure/persistence/`
- **Spring Data**: `UserJpaRepository`
- **JPA Entity**: `UserJpaEntity`

## Used By

- [[04 - Ports/inbound/RegisterUserUseCase|RegisterUserUseCase]]
- [[04 - Ports/inbound/AuthenticateUserUseCase|AuthenticateUserUseCase]]
- [[04 - Ports/inbound/RefreshTokenUseCase|RefreshTokenUseCase]]
