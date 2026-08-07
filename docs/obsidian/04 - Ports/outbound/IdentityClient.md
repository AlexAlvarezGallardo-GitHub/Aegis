---
type: port
service: aegis-bff-service
layer: domain
tags: [port, outbound, client, bff, identity]
status: implemented
port-type: outbound
---

# IdentityClient

Outbound port for communicating with the Identity Service from the BFF.

## Methods

| Method | Description |
|--------|-------------|
| `login(email, password, correlationId)` → `JsonNode` | Authenticate against Identity Service |
| `refresh(refreshToken, correlationId)` → `JsonNode` | Refresh an access token |

## Implementation

- **Adapter**: `RestIdentityClient` in `infrastructure/client/` (REST via `RestClient`)
- **Consumed by**: `BffAuthController` / `MockLoginService` in [[01 - Services/BFF Service|BFF Service]]
