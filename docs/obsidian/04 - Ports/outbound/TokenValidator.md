---
type: port
service: aegis-bff-service
layer: domain
tags: [port, outbound, bff, jwt, security]
status: implemented
port-type: outbound
---

# TokenValidator

Outbound port for validating JWT tokens and extracting their claims in the BFF.

## Methods

| Method | Description |
|--------|-------------|
| `validate(token)` → `Claims` | Validate the JWT and return its claims; throws `JwtException` on invalid/expired/wrong-type tokens |

## Implementation

- **Adapter**: `JwtTokenValidator` in `infrastructure/security/`
- **Used by**: `SessionJwtAuthenticationFilter` to authenticate downstream requests
