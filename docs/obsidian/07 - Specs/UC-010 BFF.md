---
type: spec
tags: [spec, bff, proxy]
status: implemented
uc: UC-010
branch: feature/010-bff
---

# UC-010 BFF (Backend-for-Frontend)

**Status**: ✅ Implemented

## Overview

Single entry point for the Angular SPA. Proxies auth and wallet requests, manages session-based JWT storage.

## Key Files

| Type | Location |
|------|----------|
| Spec | `specs/010-bff/spec.md` |
| Plan | `specs/010-bff/plan.md` |
| Tasks | `specs/010-bff/tasks.md` |

## Architecture

- **Service**: [[01 - Services/BFF Service\|BFF Service]]
- **Depends on**: [[01 - Services/Identity Service\|Identity Service]], [[01 - Services/Wallet Service\|Wallet Service]]
- **Depended by**: [[01 - Services/Frontend\|Frontend]]
