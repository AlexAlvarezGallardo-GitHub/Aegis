<!--
  PR title must follow: <type>(<scope>): <description>
  Types: feat, fix, refactor, test, docs, chore, ci, perf, security
  Scopes: identity, wallet, payment, fraud, notification, audit, reporting, gateway, infra, frontend
  Branch: <type>/<number>-<short-description>  |  Closes #<issue-number>
-->

## Summary

<!-- What does this PR do and why? One or two sentences. Reference the issue. -->

## Changes

<!-- List the concrete changes, grouped by area. -->

- [ ] Backend service changes
- [ ] Frontend changes
- [ ] Infrastructure / CI-CD / GitOps changes
- [ ] Documentation changes

## Testing

<!-- Describe how the change was verified. Include the commands run. -->

- [ ] `mvn clean verify` (unit + checkstyle + coverage)
- [ ] `mvn verify -Pintegration-tests` (Testcontainers)
- [ ] `npm run lint && npm run build && npm run test` (frontend)
- [ ] Manual verification steps performed

## Checklist

- [ ] Feature specification updated (`specs/<number>-<name>/`) where applicable
- [ ] OpenAPI contract updated in `specs/<feature>/contracts/` where applicable
- [ ] ADR written where an architectural decision was made
- [ ] Code follows hexagonal architecture (domain layer free of framework imports)
- [ ] No hardcoded secrets introduced
- [ ] DB migrations included and backward compatible where applicable
- [ ] Observability (metrics/traces/logs) considered for new operations
- [ ] Documentation kept in sync (service catalog, project status, vault)
- [ ] CHANGELOG.md updated for user-visible changes
- [ ] CI is green

Closes #<issue-number>
