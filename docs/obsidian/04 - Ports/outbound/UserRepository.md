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
| `findByEmail(email)` | Lookup user by email |
| `findById(userId)` | Lookup by ID |
| `save(user)` | Persist new or updated user |
| `existsByEmail(email)` | Check email uniqueness |

## Implementation

- **Adapter**: `UserRepositoryAdapter` in `infrastructure/persistence/`
- **Spring Data**: `UserJpaRepository`
- **JPA Entity**: `UserJpaEntity`
