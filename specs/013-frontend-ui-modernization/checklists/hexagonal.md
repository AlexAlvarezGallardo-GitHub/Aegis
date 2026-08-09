# Checklist: Hexagonal Architecture Compliance — **N/A for UC-013**

**Status**: NOT APPLICABLE — scope is `frontend-only` (Angular SPA).

**Rationale**: UC-013 modernizes the UI/UX of `frontend/aegis-frontend` and touches **no backend code**. Per `spec.md` §Out of Scope and the constitution §I (Hexagonal Architecture), there are no domain entities, ports, adapters, services, or JPA/Spring code in scope. Backend architectural compliance is unaffected and is enforced by the existing Checkstyle/CI gates for any future backend change.

**Cross-check**: `spec.md` §Key Entities lists only UI artifacts (Design Token, Theme, Shared Component, Shell, UI Evidence) — no domain model. `plan.md` §Constitution Check marks Principle I as N/A. `tasks.md` contains no backend paths (all under `frontend/aegis-frontend/src/`).

All CHK001–CHK010 (domain purity, port/adapter separation, layer responsibilities) are **N/A** for this feature. Nothing to verify at implementation time; re-apply if a backend change is added to this feature.
