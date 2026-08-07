# AI-Assisted Engineering Governance

> **Status:** Active. Aegis is built through an AI-assisted engineering workflow
> with **human ownership** of architecture, validation and technical decisions.

## Tools and agents

| Tool | Role |
|------|------|
| OpenCode / AI coding agents | Scaffolding, implementation, tests, docs, refactoring |
| ArchUnit + Checkstyle | Automated enforcement of architecture/quality |
| Code review (human) | Final validation of every change |

## Human responsibilities

- **Architecture**: humans decide the boundaries (hexagonal layers, service
  boundaries) and record them in ADRs.
- **Validation**: AI output is validated against the constitution
  (`.specify/memory/constitution.md`), conventions, and the Definition of Done.
- **Security**: secrets, tokens and personal data are never pasted into prompts.
- **Merge decision**: a human reviews and merges every PR; AI does not self-approve.

## Rules for using AI agents

1. **You own the output** — AI-generated code is still your code; review it.
2. **Never paste secrets** into prompts (tokens, passwords, `.env` values).
3. **Validate before merge** — run tests, checkstyle, ArchUnit; read the diff.
4. **Document AI use** — record significant AI involvement in the PR description.
5. **Treat AI output as a draft** — verify naming, tests, and behavior match the
   spec.
6. **Question hallucinations** — confirm referenced files, APIs, and versions exist.

## Known AI failure modes observed (and how they were caught)

| Failure | How it was caught |
|---------|-------------------|
| Wrong config key / nonexistent property | Build/test failure; Checkstyle |
| Suggested a swagger annotation in a controller | ArchUnit test (`controllersCarryNoSwaggerAnnotations`) |
| Referenced a nonexistent doc file | Link Checker (lychee) in CI |
| Generated a broken relative link | Link Checker |
| Bad assumptions about environment | Human review + integration tests |

## Boundaries

- AI is used for **generation and execution**; **decisions** (architecture,
  security, scope) are human.
- No AI tool is granted the authority to merge, approve, or manage secrets.
- When in doubt, ask a human.

## See also

- [`CONTRIBUTING.md`](../CONTRIBUTING.md)
- [`.specify/memory/constitution.md`](../.specify/memory/constitution.md)
- [`docs/technical-debt.md`](technical-debt.md)
