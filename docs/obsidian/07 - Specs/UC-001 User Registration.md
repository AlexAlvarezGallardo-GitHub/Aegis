---
type: spec
tags: [spec, registration, identity]
status: implemented
uc: UC-001
branch: feature/001-user-registration
---

# UC-001 User Registration

**Status**: ✅ Implemented

## Overview

Self-registration for the Aegis platform. Exposes `POST /api/v1/users/register` (unauthenticated), validates email format/uniqueness and password policy, creates the user in `PENDING_VERIFICATION` status, and publishes `UserRegistered` to `aegis.identity.user-registered` via the transactional outbox. Consumers: Audit Service (immutable audit record) and Notification Service (welcome email).

## Key Files

| Type | Location |
|------|----------|
| Spec | `specs/001-user-registration/spec.md` |
| Plan | `specs/001-user-registration/plan.md` |
| Tasks | `specs/001-user-registration/tasks.md` |
| API Contract | `specs/001-user-registration/contracts/registration-api.yaml` |
| Event Schema | `specs/001-user-registration/contracts/user-registered-event.json` |
| Data Model | `specs/001-user-registration/data-model.md` |
| Research | `specs/001-user-registration/research.md` |

## Architecture

- **Service**: [[01 - Services/Identity Service|Identity Service]]
- **Port**: [[04 - Ports/inbound/RegisterUserUseCase|RegisterUserUseCase]]
- **Model**: [[02 - Domain Models/User|User]]
- **Event**: [[03 - Domain Events/UserRegistered|UserRegistered]]

## Business Rules

1. Email MUST be RFC 5322 valid (normalized: trim + lowercase) and globally unique (DB unique constraint → `409 EMAIL_ALREADY_REGISTERED`)
2. Password: 8–128 chars, at least 1 uppercase, 1 lowercase, 1 digit, 1 special char; hashed with BCrypt (strength ≥ 10)
3. New accounts start in `PENDING_VERIFICATION` with a UUID v7 `userId` and UTC `registeredAt`
4. Event published via transactional outbox (same DB transaction as the user insert)

## API

`POST /api/v1/users/register` → `201 Created` `{ userId, email, status, registeredAt }`

Errors: `400` validation (`INVALID_EMAIL_FORMAT`, `PASSWORD_TOO_SHORT`, etc.), `409 EMAIL_ALREADY_REGISTERED`.

## Events

| Event | Topic | Producer | Consumers |
|-------|-------|----------|-----------|
| [[03 - Domain Events/UserRegistered|UserRegistered]] | `aegis.identity.user-registered` | Identity | Audit, Notification |

```mermaid
sequenceDiagram
    participant Client
    participant GW as API Gateway
    participant ID as Identity Service
    participant DB as PostgreSQL
    participant Kafka as Kafka (outbox)
    participant Audit as Audit Service
    participant Notif as Notification Service

    Client->>GW: POST /api/v1/users/register
    GW->>ID: forward request
    ID->>ID: validate email format + password policy
    ID->>DB: INSERT user (PENDING_VERIFICATION)
    ID->>DB: INSERT outbox (UserRegistered) [same TX]
    DB-->>Kafka: OutboxRelay → aegis.identity.user-registered
    Kafka->>Audit: persist audit record
    Kafka->>Notif: send welcome email
    ID-->>GW: 201 Created {userId, email, status, registeredAt}
    GW-->>Client: 201 Created
```
