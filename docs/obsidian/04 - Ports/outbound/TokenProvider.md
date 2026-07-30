---
type: port
service: aegis-identity-service
layer: domain
tags: [port, outbound, jwt, security]
status: implemented
port-type: outbound
---

# TokenProvider

Outbound port for JWT token generation and validation.

## Methods

| Method | Description |
|--------|-------------|
| `generateAccessToken(user)` | Short-lived JWT (15 min) |
| `generateRefreshToken(user)` | Long-lived JWT (7 days) |
| `validateAccessToken(token)` | Parse and validate |
| `validateRefreshToken(token)` | Parse and validate |
| `getUserIdFromToken(token)` | Extract user ID |

## Implementation

- **Adapter**: `JwtTokenProvider` in `infrastructure/security/`
- **Library**: JJWT 0.12.x
- **Signing**: HMAC-SHA256 with configurable secret
