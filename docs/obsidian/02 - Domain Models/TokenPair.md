---
type: value-object
service: aegis-identity-service
layer: domain
tags: [ddd, value-object, jwt, auth]
status: implemented
---

# TokenPair

Value object containing the access token and the opaque refresh token returned by a token refresh.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `accessToken` | String | JWT access token (short-lived) |
| `refreshToken` | String | Opaque refresh token (long-lived) |

## Token Configuration

| Parameter | Access Token | Refresh Token |
|-----------|-------------|---------------|
| Expiry | 15 minutes | 7 days |
| Format | Signed JWT | Opaque value, SHA-256 hashed at rest |

## Used By

- [[04 - Ports/inbound/RefreshTokenUseCase|RefreshTokenUseCase]] (result of refresh rotation)
- `RefreshTokenService` (constructs the pair after rotation)
- Not issued at login — [[04 - Ports/inbound/AuthenticateUserUseCase|AuthenticateUserUseCase]] returns only an access token
