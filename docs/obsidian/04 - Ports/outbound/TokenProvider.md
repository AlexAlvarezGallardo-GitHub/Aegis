---
type: port
service: aegis-identity-service
layer: domain
tags: [port, outbound, jwt, security]
status: implemented
port-type: outbound
---

# TokenProvider

Outbound port for JWT access token generation and validation.

## Methods

| Method | Description |
|--------|-------------|
| `generateAccessToken(UserId userId, String email)` → `String` | Short-lived JWT access token |
| `validateAccessToken(token)` → `UserId` | Parse and validate, extract user ID |
| `getAccessTokenExpirySeconds()` → `long` | Configured access token expiry in seconds |

## Implementation

- **Adapter**: `JwtTokenProvider` in `infrastructure/security/`
- **Library**: JJWT
- **Signing**: HMAC-SHA256 with configurable secret
- **Expiry**: access token 15 minutes (default); refresh tokens are opaque and managed via [[04 - Ports/outbound/RefreshTokenRepository|RefreshTokenRepository]], not by this port

## Used By

- [[04 - Ports/inbound/AuthenticateUserUseCase|AuthenticateUserUseCase]]
- [[04 - Ports/inbound/RefreshTokenUseCase|RefreshTokenUseCase]]
