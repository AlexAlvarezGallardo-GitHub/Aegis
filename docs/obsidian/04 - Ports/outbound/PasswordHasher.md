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
| `hash(rawPassword)` | Returns BCrypt hash |
| `matches(rawPassword, hash)` | Verifies against hash |

## Implementation

- **Adapter**: `BCryptPasswordHasher` in `infrastructure/security/`
- **Algorithm**: BCrypt with strength 10
