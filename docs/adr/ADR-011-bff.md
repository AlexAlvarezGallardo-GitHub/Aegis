# ADR-011: Backend-for-Frontend (BFF) for the Web Client

## Status

Accepted

## Date

2026-08-07

## Context

The Angular frontend needs a consistent, secure API surface. Calling the
microservices directly would force the browser to hold JWT access tokens and
re-implement auth/session logic per service, and would couple the frontend to
service-specific endpoints.

## Decision

**Introduce a BFF service (`aegis-bff-service`) as the single entry point for the
web client.**

- The BFF owns the browser session (HttpOnly cookies) and proxies requests to the
  backend services.
- Authentication uses HttpOnly session cookies + server-side token storage; the
  browser never sees the JWT.
- The BFF aggregates responses for the UI (e.g. wallet details) and maps errors to a
  consistent API contract.

## Alternatives Considered

### Alternative 1: Frontend calls microservices directly
- **Pros**: no extra service.
- **Cons**: JWT in the browser; duplicated auth logic; service endpoint coupling;
  wider attack surface.

### Alternative 2: API Gateway that proxies + secures
- **Pros**: central routing.
- **Cons**: a generic gateway does not aggregate responses for the UI; the BFF
  pattern fits the web client better.

**Why not chosen**: the BFF is purpose-built for the browser client, keeping the
JWT server-side and aggregating UI-specific responses.

## Consequences

### Positive
- Browser holds only a session cookie; JWT stays server-side.
- Single, frontend-oriented API contract.
- Cross-cutting concerns (CSRF, session) live in one place.

### Negative
- An extra service to operate.
- The BFF is a potential single point for the web client (mitigated by health
  checks and the observability stack).

### Risks
- **Risk**: BFF becomes a god-service — **Mitigation**: it proxies/aggregates only;
  business logic stays in the domain services.

## Related Decisions

- ADR-005 (Kafka backbone) — the BFF is not on the event backbone for business data.

## References

- `specs/010-bff/spec.md`
- `specs/010-bff/contracts/bff-api.yaml`
