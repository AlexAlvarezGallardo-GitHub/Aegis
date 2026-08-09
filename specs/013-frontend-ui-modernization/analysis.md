# Analysis: UC-013 Frontend UI/UX Modernization

**Feature**: 013-frontend-ui-modernization | **Branch**: `feature/013-frontend-ui-modernization`
**Purpose**: SDD step 6 — cross-artifact consistency and quality validation of spec ↔ research ↔ plan ↔ tasks ↔ issues.

## 1. Scope

Validated artifacts: `spec.md` (8 user stories, 17 FRs, 5 DF items, 8 acceptance-focused scenarios sets, 13 assumptions), `research.md` (token mapping + defect list), `plan.md` (11 task groups, design model, constitution check), `tasks.md` (73 tasks, 12 phases), GitHub issues #224–#236.

## 2. Traceability matrices

### 2.1 User stories ↔ tasks

| Spec user story | Priority | Task group | Tasks | Tag count | Status |
|-----------------|----------|------------|-------|-----------|--------|
| US-1 Shell & Navigation | P1 | Phase 2 | T016–T021 | 6 (`US1`) | ✅ |
| US-2 Shared Components | P1 | Phase 3 | T022–T031 | 10 (`US2`) | ✅ |
| US-3 Authentication | P2 | Phase 4 | T032–T036 | 5 (`US3`) | ✅ |
| US-4 Wallet Experience | P2 | Phase 5 | T037–T044 | 8 (`US4`) | ✅ |
| US-5 Placeholder Pages | P3 | Phase 6 | T045–T046 | 2 (`US5`) | ✅ |
| US-6 Responsive | P2 | Phase 7 | T047–T049 | 3 (`US6`) | ✅ |
| US-7 Accessibility | P3 | Phase 8 | T050–T055 | 6 (`US7`) | ✅ |
| US-8 Evidence Refresh | P3 | Phase 11 | T069–T073 | 5 (`US8`) | ✅ |
| Foundation (prereq) | — | Phase 1 | T005–T015 | 11 (`FOUND`) | ✅ |
| Bug backlog | — | Phase 9 | T056–T065 | 10 (`X`) | ✅ |
| Polish | — | Phase 10 | T066–T068 | 3 (`POL`) | ✅ |

**All 73 tasks carry a story tag** after the Analyze fixes (previously T009–T015 were untagged). No orphan tasks; no story without tasks.

### 2.2 Functional requirements ↔ tasks

| FR | Covered by | Status |
|----|-----------|--------|
| FR-001 zero hex outside tokens | T005, T008, gate grep (added in Analyze) | ✅ |
| FR-002 zero undefined `var()` | T005–T015, gate grep (added in Analyze) | ✅ |
| FR-003 state coverage | T022–T031 | ✅ |
| FR-004 tabular-nums + currency pipe | T006, T040 | ✅ |
| FR-005 no overflow 390px | T047–T049 | ✅ |
| FR-006 mobile drawer | T017 | ✅ |
| FR-007 no fake notifications | T019 | ✅ |
| FR-008 no `setTimeout` login | T033 | ✅ |
| FR-009 registration links | T034 | ✅ |
| FR-010 receipt/sign/premium tokens | T037–T039 | ✅ |
| FR-011 dead actions disabled | T041 | ✅ |
| FR-012 focus trap/overlay | T023, T042, T051 | ✅ |
| FR-013 a11y (skip/labels/aria/semantic) | T050–T053 | ✅ |
| FR-014 reduced motion | T055 | ✅ |
| FR-015 placeholders tokenized | T045–T046 | ✅ |
| FR-016 evidence regeneration | T069–T073 (G8) | ✅ |
| FR-017 preserve functionality | phase gates + checkpoints (transversal) | ✅ |
| DF-001..005 token/theme/registry | T005–T014 | ✅ |

### 2.3 Plan task groups ↔ tasks phases ↔ issues

| Plan group | tasks.md phase | Issue | Status |
|------------|----------------|-------|--------|
| T0 Baseline | Phase 0 | #225 | ✅ |
| T1 Tokens & theme | Phase 1 | #226 | ✅ |
| T2 Shell & nav | Phase 2 | #227 | ✅ |
| T3 Components | Phase 3 | #228 | ✅ |
| T4 Auth | Phase 4 | #229 | ✅ |
| T5 Wallets | Phase 5 | #230 | ✅ |
| T6 Placeholders | Phase 6 | #231 | ✅ |
| T7 Responsive | Phase 7 | #232 | ✅ |
| T8 Accessibility | Phase 8 | #233 | ✅ |
| T9 Bug backlog | Phase 9 | #234 | ✅ |
| T10 Polish | Phase 10 | #235 | ✅ |
| T11 Evidence (G8) | Phase 11 | #236 | ✅ |

## 3. Structural checks

| Check | Result |
|-------|--------|
| Task IDs | 73 tasks, sequential T001–T073, zero duplicates, zero gaps (verified by script) |
| Phase checkpoints | every phase (0–11) ends with an explicit Checkpoint/Gate |
| Issue hierarchy | epic #224 + 12 sub-issues; all children in epic task list; parent ref on each; deps (`Depends on`/`Blocks`) resolved |
| Priority cascade | epic high → #226 critical → #227/#228 high → rest medium/low ✅ |
| Contracts/`data-model.md` | intentionally absent (frontend-only, no API/data-model changes) — consistent across spec, plan, tasks |
| Decisions D1/D2/A7–A10 | present in spec (Clarifications), research (§5), plan (§Design model), tasks (T005–T010, T040, T041, T019) |

## 4. Gaps found & fixed during Analyze

| # | Gap | Fix |
|---|-----|-----|
| 1 | FR-001/FR-002 grep gates (zero hex, zero undefined `var()`) were not operationalized in the quality gate | Added both greps to the Checkpoint definition in `tasks.md` (runs every phase) |
| 2 | T009–T015 (theme/global/validation) had no story tag | Tagged `[FOUND]` for full traceability |
| 3 | Evidence tasks used tag `[EV]` while spec names it US-8 | Renamed to `[US8]` for consistent spec↔tasks mapping |

## 5. Non-blocking notes

- **US-7 (P3)** maps to issue #233 labeled `priority-medium`. Justified: accessibility as a quality bar deserves medium over low; does not break the priority cascade (all children still ≥ the epic's requirement).
- **US-2 & US-1 are both P1** and parallelizable after Phase 1 — flagged in tasks.md parallel opportunities; staffing decision at implement time.
- `enableMockLogin` and the no-op `http-auth.interceptor` remain flagged for team review (A4) — out of scope, no task assigned by design.

## 6. Conclusion

Artifacts are consistent and traceable end-to-end: every user story, FR, and DF maps to concrete tasks; every task group maps to a phase and a GitHub sub-issue; the dependency chain Phase 0 → 1 → (2/3) → 4 → 5 → 6/7/8 → 9 → 10 → 11 (G8, blocks close) is explicit. No blocking gaps remain. **Ready for SDD step 7 (Checklist).**
