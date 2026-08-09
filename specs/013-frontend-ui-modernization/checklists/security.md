# Checklist: Security Requirements Quality — Frontend-Only Adaptation (UC-013)

**Scope**: `frontend-only`. Items that apply to the SPA are active checks; backend-only items are marked **N/A** with rationale (auth mechanics are out of scope per `spec.md` A4). Checklist validates **requirements quality** against `spec.md`, not implementation correctness.

## Category: Authentication & Authorization (frontend surface)

- [ ] CHK030 — Are authentication requirements for the app surface specified without contradicting the existing BFF cookie-session model? [Clarity, Spec §Assumptions A4] (Backend `@PreAuthorize`/JWT issuance is N/A — BFF-owned.)
- [ ] CHK031 — **N/A** — Refresh token rotation is BFF-side; no token handling is added to the frontend in this feature. [Consistency, Spec §Assumptions A4]
- [ ] CHK032 — **N/A** — RBAC roles/permissions are enforced server-side; the frontend only reflects the authenticated session. [Consistency, Spec §Out of Scope]
- [ ] CHK033 — **N/A** — `@PermitAll` endpoint marking is backend-only; not in scope. [Consistency, Spec §Out of Scope]

## Category: Data Protection (client side)

- [ ] CHK034 — **N/A** — Encryption-at-rest is a backend/DB concern. [Consistency, Spec §Out of Scope]
- [ ] CHK035 — Are requirements explicit that **no secrets/credentials** may be hardcoded or committed in the frontend (env vars with placeholders only; `post-edit-security` plugin + gitleaks enforced)? [Completeness, Constitution §IV, Spec §Requirements]
- [ ] CHK036 — Are requirements explicit that the frontend must not log or toast sensitive data (credentials, tokens, full PII)? [Completeness, Constitution §IV, Spec §Functional Requirements]

## Category: Application Security (client side)

- [ ] CHK037 — **N/A** — Rate limiting is enforced by BFF/API Gateway, not the SPA. [Consistency, Spec §Out of Scope]
- [ ] CHK038 — Is the requirement to keep Angular's built-in XSS protections in place (no `innerHTML` with untrusted data, no bypassing `DomSanitizer`) specified? [Completeness, Constitution §IV, Spec §Functional Requirements]
- [ ] CHK039 — **N/A** — CORS is handled by the proxy/BFF; the frontend uses relative `/api/...` URLs (no change). [Consistency, Spec §Assumptions A5]
- [ ] CHK040 — Are error requirements explicit that user-facing errors must show friendly messages (via `HttpErrorInterceptor`/toasts) and never raw stack traces or internal details? [Consistency, Constitution §IV, Spec §Functional Requirements]

## Review note

Security enforcement for this feature: `security-reviewer` agent checks the FR-017/auth-mechanics guardrails and the CHK035/036/038/040 client-side items during the `review-implement` gate. No secrets must be introduced anywhere (post-edit-security + gitleaks).
