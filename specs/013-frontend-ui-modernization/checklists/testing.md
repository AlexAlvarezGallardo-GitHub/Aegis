# Checklist: Testing Requirements Quality — Frontend-Only Adaptation (UC-013)

**Scope**: `frontend-only`. Adapts constitution Principle V (Test-Driven Quality) to the Angular SPA: Karma/Jasmine unit specs + Playwright e2e + evidence per `AGENTS.md` tiers. Validates **requirements quality** against `spec.md` and `tasks.md`.

## Category: Unit Testing (Karma/Jasmine)

- [ ] CHK051 — Are unit spec requirements specified for components/services touched by the redesign (auth, registration, wallet, shell, header, sidebar, shared components, theme/registry changes)? [Completeness, Constitution §V, Spec §Success Criteria SC-001]
- [ ] CHK052 — Is the Arrange-Act-Assert structure required for new/updated specs? [Consistency, Constitution §V]
- [ ] CHK053 — Are test naming conventions specified (descriptive `should ...` style) for updated specs? [Clarity, Spec §Success Criteria]
- [ ] CHK054 — Is one spec file per component/service maintained (no god spec files)? [Consistency, Constitution §V]

## Category: Component State & UX Testing (frontend-specific)

- [ ] CHK055 — Do requirements cover the frontend testing conventions from AGENTS.md: submit-button disabled on `form.invalid || isLoading`, invalid-submit snackbar, `isLoading` reset via `finalize` (success/error/timeout), HTTP timeout handled (no infinite spinner)? [Completeness, AGENTS.md Frontend Testing Conventions, Spec §Success Criteria]
- [ ] CHK056 — Do requirements cover 4xx/5xx and network-error handling (HttpErrorResponse without status) in wallet/auth specs? [Coverage, AGENTS.md, Spec §Edge Cases]
- [ ] CHK057 — Do requirements cover relative URL usage through the proxy (no absolute backend URLs) in service specs? [Consistency, AGENTS.md, Spec §Assumptions A5]
- [ ] CHK058 — **N/A** — Testcontainers (PostgreSQL/Kafka/Redis) are backend concerns; frontend uses mocked HTTP (`HttpTestingController`). [Consistency, Spec §Out of Scope]
- [ ] CHK059 — **N/A** — MockMvc/WebTestClient are backend tools; not in scope. [Consistency, Spec §Out of Scope]

## Category: E2E (Playwright)

- [ ] CHK060 — Is the full e2e suite (`e2e/`, Playwright against the live stack) required green — **zero regressions in auth/wallet flows**? [Completeness, Spec §Success Criteria SC-002]
- [ ] CHK061 — Are the critical paths flagged for exhaustive coverage: login, registration, create wallet, deposit end-to-end incl. idempotency 409 duplicate-reference? [Completeness, Spec §Edge Cases, §Success Criteria]
- [ ] CHK062 — **N/A** — WireMock/consumer-driven contract tests and Kafka contract tests are backend concerns; not in scope. [Consistency, Spec §Out of Scope]

## Category: Coverage & Evidence

- [ ] CHK063 — Is a coverage expectation specified for specs of touched components/services (measure and report; no target below the existing baseline)? [Measurability, Constitution §V, Spec §Success Criteria]
- [ ] CHK064 — Is per-phase evidence required (`evidence/unit/ui-modernization-unit.md`: scope, exact commands, pass/fail, screenshots) per AGENTS.md? [Completeness, AGENTS.md Test Tiers]
- [ ] CHK065 — Is UI evidence regeneration (gate G8, T069–T073) required so no stale old-design screenshots remain tracked? [Completeness, Spec §Success Criteria SC-006, FR-016]

## Review note

`test-engineer` executes generated/updated specs and must hand off green; `service-builder`-equivalent frontend work runs `npm test` + `npm run lint` per phase gate. Evidence reports are committed per phase.
