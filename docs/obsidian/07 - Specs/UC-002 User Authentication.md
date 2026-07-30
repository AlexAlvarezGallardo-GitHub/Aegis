---
type: spec
tags: [spec, auth, identity]
status: implemented
uc: UC-002
branch: feature/002-user-authentication
---

# UC-002 User Authentication

**Status**: ✅ Implemented

## Overview

User login with JWT tokens, account lockout after 5 failed attempts. Exposes `POST /api/v1/auth/login` and `POST /api/v1/auth/refresh`.

## Key Files

| Type | Location |
|------|----------|
| Spec | `specs/002-user-authentication/spec.md` |
| Plan | `specs/002-user-authentication/plan.md` |
| Tasks | `specs/002-user-authentication/tasks.md` |
| API Contract | `specs/002-user-authentication/contracts/auth-api.yaml` |
| Event Schemas | `contracts/user-authenticated-event.json`, `user-account-locked-event.json` |
| Data Model | `specs/002-user-authentication/data-model.md` |

## Architecture

- **Service**: [[01 - Services/Identity Service\|Identity Service]]
- **Port**: [[04 - Ports/inbound/AuthenticateUserUseCase\|AuthenticateUserUseCase]]
- **Models**: [[02 - Domain Models/Credentials\|Credentials]], [[02 - Domain Models/TokenPair\|TokenPair]]
- **Events**: [[03 - Domain Events/UserAuthenticated\|UserAuthenticated]], [[03 - Domain Events/UserAccountLocked\|UserAccountLocked]]
