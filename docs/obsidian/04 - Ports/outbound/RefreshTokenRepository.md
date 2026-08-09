---
type: port
service: aegis-identity-service
layer: domain
tags: [port, outbound, repository, jwt, auth]
status: implemented
port-type: outbound
---

# RefreshTokenRepository

Outbound port for managing refresh token persistence and lifecycle (rotation).

## Methods

| Method | Description |
|--------|-------------|
| `findByTokenHash(tokenHash)` → `Optional<StoredRefreshToken>` | Find a stored token by its SHA-256 hash |
| `save(StoredRefreshToken token)` | Persist a new refresh token |
| `revoke(tokenHash)` | Mark the given refresh token as revoked |

## Data Object

```java
record StoredRefreshToken(UUID id, String tokenHash, UserId userId,
                          Instant expiresAt, Instant revokedAt, Instant createdAt) {}
```

## Implementation

- **Adapter**: in `infrastructure/persistence/`
- **Token format**: opaque value; only the SHA-256 hash is persisted
- **Expiry**: 7 days (configurable `aegis.jwt.refresh-token-expiration-ms`)

## Used By

- [[04 - Ports/inbound/RefreshTokenUseCase|RefreshTokenUseCase]] (rotation)
