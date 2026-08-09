---
type: port
service: aegis-bff-service
layer: domain
tags: [port, outbound, bff, jwt, security]
status: implemented
port-type: outbound
---

# JwtSigningKey

Outbound port that provides the HMAC signing key used to sign and verify JWTs in the BFF.

## Methods

| Method | Description |
|--------|-------------|
| `get()` → `SecretKey` | The secret key used for signing/verifying JWTs |

## Implementation

- **Adapter**: `BffJwtSigningKey` in `infrastructure/security/` (reads the configured secret)
- Keeps the application layer free of framework and configuration imports
