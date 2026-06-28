---
name: api-design
description: Use when designing REST APIs. Generates OpenAPI 3 contracts, controller skeletons, DTOs, error handling, and pagination following Aegis API conventions.
---

# API Design

Design REST APIs following Aegis conventions and generate OpenAPI 3 contracts.

## Input

The user provides:
- Resource name (e.g., `wallet`, `payment`, `merchant`)
- Operations needed (CRUD, custom actions)
- Related resources and relationships

## API Conventions

### URL Structure

- Base path: `/api/v1/<resource>`
- Plural nouns for collections: `/api/v1/wallets`
- Singular for single resources: `/api/v1/wallets/{id}`
- Sub-resources: `/api/v1/wallets/{id}/transactions`

### Standard Operations

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/<resources>` | List with pagination |
| GET | `/api/v1/<resources>/{id}` | Get by ID |
| POST | `/api/v1/<resources>` | Create |
| PUT | `/api/v1/<resources>/{id}` | Full update |
| PATCH | `/api/v1/<resources>/{id}` | Partial update |
| DELETE | `/api/v1/<resources>/{id}` | Delete |

### Pagination & Error Handling

Follow pagination (`page`, `size`, `sort`) and error response conventions per `.specify/memory/constitution.md` §Constraints.

## Generation Output

1. **OpenAPI 3 specification** (`api/<service>-api.yaml`)
2. **Controller skeleton** with all endpoints
3. **Request/Response DTOs** as Java records
4. **Exception handler** with standard error mapping
5. **Validation annotations** on request DTOs
