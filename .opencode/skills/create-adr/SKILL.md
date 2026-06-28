---
name: create-adr
description: Use when creating an Architecture Decision Record. Generates ADRs following the project's template with context, decision, consequences, and alternatives considered.
---

# Create ADR

Generate an Architecture Decision Record following Aegis conventions.

## Input

The user provides:
- Decision title
- Brief context about the architectural problem
- The decision made (or to be evaluated)

## ADR Template

Generate `docs/adr/ADR-<NNN>-<title-slug>.md`:

```markdown
# ADR-<NNN>: <Title>

## Status

<Proposed | Accepted | Deprecated | Superseded by ADR-XXX>

## Date

<YYYY-MM-DD>

## Context

Describe the architectural problem or challenge. Include:
- What is the issue that motivates this decision?
- What are the constraints?
- What forces are at play?

## Decision

State the decision clearly. Include:
- What was decided?
- Why this approach?
- What alternatives were considered?

## Alternatives Considered

### Alternative 1: <Name>
- Description
- Pros
- Cons
- Why not chosen

### Alternative 2: <Name>
- Description
- Pros
- Cons
- Why not chosen

## Consequences

### Positive
- Benefit 1
- Benefit 2

### Negative
- Tradeoff 1
- Tradeoff 2

### Risks
- Risk 1 with mitigation strategy
- Risk 2 with mitigation strategy

## Related Decisions

- ADR-XXX: <related decision>

## References

- Links to relevant documentation, research, or external resources
```

## Rules

1. Number ADRs sequentially (ADR-001, ADR-002, ...)
2. Use kebab-case for filenames
3. One decision per ADR
4. Be explicit about tradeoffs
5. Reference related ADRs
6. Include date and status
7. Store in `docs/adr/` directory
