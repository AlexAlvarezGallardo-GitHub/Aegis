---
type: port
service: aegis-bff-service
layer: domain
tags: [port, outbound, bff, jwt, session]
status: implemented
port-type: outbound
---

# TokenStore

Outbound port for storing and retrieving JWT tokens associated with the current user session in the BFF.

## Methods

| Method | Description |
|--------|-------------|
| `storeTokens(accessToken, refreshToken)` | Store the token pair for the current session |
| `getAccessToken()` → `Optional<String>` | Return the access token if present |
| `getRefreshToken()` → `Optional<String>` | Return the refresh token if present |
| `clear()` | Clear all tokens from the current session |

## Implementation

- **Adapter**: `SessionJwtStore` in `application/service/` (session-backed)
