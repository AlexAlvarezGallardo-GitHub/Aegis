---
type: port
service: aegis-identity-service
layer: domain
tags: [port, outbound, security]
status: implemented
port-type: outbound
---

# PasswordHasher

Outbound port for password hashing and verification.

## Methods

| Method | Description |
|--------|-------------|
| `hash(rawPassword)` → `PasswordHash` | Returns a BCrypt hash of the plain-text password |
| `matches(rawPassword, hash)` → `boolean` | Verifies a plain-text password against a stored [[02 - Domain Models/PasswordHash\|PasswordHash]] |

## Implementation

- **Adapter**: `BCryptPasswordHasher` in `infrastructure/security/`
- **Algorithm**: BCrypt with strength 10
- **Input**: plain-text password validated by the [[02 - Domain Models/Password|Password]] value object

## Used By

- [[04 - Ports/inbound/RegisterUserUseCase|RegisterUserUseCase]]
- [[04 - Ports/inbound/AuthenticateUserUseCase|AuthenticateUserUseCase]]
- [[02 - Domain Models/User|User]] aggregate (`User.register`, `User.authenticate`)
