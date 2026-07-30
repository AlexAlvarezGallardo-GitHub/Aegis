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

User self-registration for the Aegis platform. Exposes `POST /api/v1/users/register`, validates input, creates user in `PENDING_VERIFICATION` status, publishes `UserRegistered` event.

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

- **Service**: [[01 - Services/Identity Service\|Identity Service]]
- **Port**: [[04 - Ports/inbound/RegisterUserUseCase\|RegisterUserUseCase]]
- **Model**: [[02 - Domain Models/User\|User]]
- **Event**: [[03 - Domain Events/UserRegistered\|UserRegistered]]
