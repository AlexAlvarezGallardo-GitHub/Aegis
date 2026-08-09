---
type: value-object
service: aegis-identity-service
layer: domain
tags: [ddd, value-object, uuid]
status: implemented
---

# UserId

Value object wrapping a UUID v7 identifier.

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `value` | UUID | UUID v7 (time-ordered) |

## Factory Methods

- `UserId.generate()` → new identifier via `UuidV7Generator`
- `UserId.of(UUID value)` → wraps an existing value

## Generation

- Uses `UuidV7Generator` from [[01 - Services/Common Module|Common Module]]
- Time-ordered for DB index performance

## Used By

- [[02 - Domain Models/User|User]] aggregate root
- [[04 - Ports/outbound/UserRepository|UserRepository]] (lookup key)
- [[04 - Ports/outbound/RefreshTokenRepository|RefreshTokenRepository]] (`StoredRefreshToken.userId`)
- JPA entity ID
- Kafka event payloads

## JPA Mapping

Stored as `UUID` in `users.id` column.
