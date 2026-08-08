# Feature Specification: 012 — Docs Alignment Orchestration

**Feature Branch**: `feature/012-docs-alignment`

**Created**: 2026-08-08

**Status**: Draft

**Input**: User request — un agente que orqueste y asegure que la documentación entre los repositorios (Aegis, Aegis-GitOps, Aegis-Portfolio) esté alineada y refleje siempre la realidad.

## User Scenarios & Testing

### User Story 1 — Registro canónico como única fuente de verdad (Priority: P1)

Como ingeniero, quiero que todos los claims públicos (servicios, estados, entornos, cobertura GitOps, métricas) estén definidos una sola vez en un registro parseable, para que README, project-status, vault y portfolio no puedan divergir.

**Why this priority**: Es la base del mecanismo; sin una fuente de verdad ejecutable, el resto es best-effort.

**Independent Test**: `node scripts/check-doc-alignment.mjs` pasa sin drift con el estado actual y falla si se borra un módulo de `backend/` o un chart de GitOps sin actualizar el registro.

**Acceptance Scenarios**:

1. **Given** `docs/architecture/platform-registry.json` con `schemaVersion: 1`, **When** se ejecuta el checker, **Then** valida que cada `module` declarado existe en `backend/` y que la cobertura de charts/applications coincide con Aegis-GitOps.
2. **Given** un claim prohibido (`enterprise-grade`, `multi-tenant`, `cryptographically-linked audit`, `no manual deployments exist`) en README/project-status/service-catalog, **When** se ejecuta el checker, **Then** falla indicando `file:line`.

### User Story 2 — CI gate anti-drift (Priority: P1)

Como reviewer, quiero que un workflow de CI falle cualquier PR que desalinee código, GitOps o docs, para que el drift nunca llegue a main.

**Why this priority**: Detección determinista continua, no dependiente de invocar a un agente.

**Independent Test**: `docs-drift.yml` corre en pull requests (paths `backend/**`, `docs/architecture/**`) y en nightly; devuelve exit≠0 con drift.

**Acceptance Scenarios**:

1. **Given** un PR que añade un nuevo módulo backend sin registro, **When** corre `docs-drift`, **Then** el check falla con "backend module on disk but not declared in registry".
2. **Given** un PR que añade un chart en Aegis-GitOps sin actualizar el registro, **When** corre `docs-drift`, **Then** falla con "Aegis-GitOps has chart not declared in registry".

### User Story 3 — Agente orquestador `docs-alignment` (Priority: P2)

Como ingeniero, quiero un agente que, al cerrar una feature, propague el registro a los tres repos y abra PRs, para no mantener claims manualmente.

**Why this priority**: Aporta la propagación semántica (vault, portfolio, GitOps README) que un check determinista no puede hacer.

**Independent Test**: Invocando al agente con "Audit docs drift", devuelve un informe `file:line` de divergencias contra el registro.

**Acceptance Scenarios**:

1. **Given** el agente con acceso a los tres repos públicos, **When** se le pide "Sync docs across repos", **Then** actualiza el registro, regenera `site.ts` del portfolio y abre PRs en Aegis-Portfolio y Aegis-GitOps.
2. **Given** un drift que no puede arreglar en sesión, **When** completa el audit, **Then** abre una issue `docs-drift` en el repo afectado.

### User Story 4 — Portfolio consumidor del registro (Priority: P2)

Como reclutador/ingeniero externo, quiero que el portfolio no contenga números ni estados hardcodeados, para que lo que veo sea la realidad del código.

**Why this priority**: Elimina la fuente de los claims que OpenAI detectó (9 services, 169 commits, Fraud Planned).

**Independent Test**: `src/data/site.ts` se regenera desde el registro en build; los counters de `Hero.astro` y `Process.astro` salen del registro o de métricas live.

**Acceptance Scenarios**:

1. **Given** el registro actualizado a 6 servicios backend, **When** se construye el portfolio, **Then** el contador de servicios muestra 6 y Fraud aparece como Built.
2. **Given** la métrica `commitsOnMain=140`, **When** se construye el portfolio, **Then** el stat de commits muestra 140 (o el valor live del dashboard), nunca 169.

### Edge Cases

- ¿Qué pasa si Aegis-GitOps no está disponible en el momento del check? → el checker clona el repo público; si el clon falla (red), el check falla con mensaje claro y el PR no se bloquea en silencio.
- ¿Qué pasa si un módulo es una librería (no desplegable)? → `sharedLibraries` se valida en `backend/` pero nunca se exige chart/application.
- ¿Cómo se evita el falso positivo del claim prohibido cuando el texto lo usa en negativo ("no es enterprise-grade")? → se audita con el agente; el checker usa coincidencia de frase exacta y es revisado manualmente si aparece.
- ¿Qué pasa con un servicio `partial` (reporting)? → el registro admite `status: partial` y exige coherencia en la matriz de entornos.

## Requirements

### Functional Requirements

- **FR-001**: El registro `docs/architecture/platform-registry.json` DEBE declarar servicios, estados, entornos, cobertura GitOps, métricas y capacidades booleans honestas.
- **FR-002**: El checker DEBE validar `backend/` contra el registro (módulos faltantes y no declarados).
- **FR-003**: El checker DEBE validar Aegis-GitOps (charts, `applications/dev`, `applications/pre|stage|prod`, `app-of-apps-dev.yaml`) contra el registro.
- **FR-004**: El checker DEBE fallar ante claims prohibidos en README, project-status y service-catalog (coincidencia exacta de `forbiddenClaimPhrases`).
- **FR-005**: El workflow `docs-drift.yml` DEBE ejecutarse en PR con paths relevantes, nightly y dispatch, y devolver exit≠0 ante drift.
- **FR-006**: El agente `docs-alignment` DEBE actualizar el registro antes de propagar cualquier claim.
- **FR-007**: El agente DEBE usar `gh` CLI con token por repo (nunca GitHub MCP) para PRs cross-repo.
- **FR-008**: El portfolio DEBE regenerar `src/data/site.ts` desde el registro (build step) y NO hardcodear contadores/estados.
- **FR-009**: Los docs de Aegis (README, project-status, service-catalog, vault) DEBEN actualizarse en el mismo commit que el registro.

### Key Entities

- **platform-registry.json**: fuente de verdad canónica (schemaVersion, platform, repositories, environments, artifacts, gitops, metrics).
- **check-doc-alignment.mjs**: checker estructural Node sin dependencias.
- **docs-drift.yml**: workflow de CI gate.
- **docs-alignment agent**: orquestador de propagación y auditoría semántica.

## Success Criteria

### Measurable Outcomes

- **SC-001**: `node scripts/check-doc-alignment.mjs` devuelve OK con el estado actual y falla ante cada tipo de drift inyectado (módulo, chart, aplicación, claim prohibido).
- **SC-002**: El workflow `docs-drift.yml` pasa en CI y falla en un PR con drift.
- **SC-003**: El portfolio muestra 6 servicios y Fraud=Built, y ningún claim prohibido, tras el P0 de alineación.
- **SC-004**: Una auditoría del agente (`docs-alignment`) reporta cero divergencias `file:line` contra el registro en los tres repos.

## Assumptions

- Los tres repos son públicos, por lo que el checker puede clonar Aegis-GitOps sin token.
- El repositorio local Aegis ya está alineado en README (6 servicios, Reporting partial); el trabajo pendiente es el portfolio y GitOps.
- Los claims que OpenAI detectó viven en Aegis-Portfolio (site.ts, Hero.astro, Process.astro), no en el repo Aegis.
- Se seguirá la convención spec-driven y el flujo GitHub Flow con PRs por repo.
