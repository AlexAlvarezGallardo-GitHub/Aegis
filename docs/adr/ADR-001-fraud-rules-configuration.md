# ADR-001: Fraud Rules Configuration Strategy and Extensibility

## Status

Accepted

## Date

2026-07-31

## Context

The Fraud Service must evaluate transactions against a set of business rules (velocity, amount threshold, geographic, time-based). These rules change frequently as fraud patterns evolve, and new rules must be added without requiring redeployment or code changes.

Key constraints:
- Rules must be configurable at runtime without code changes
- Rule weights drive the composite risk score (0-100)
- The engine must remain extensible for new rule types
- Assessment must complete in under 200ms

## Decision

**Rule definitions live in the database** (`fraud_rules` table), while **rule evaluation logic lives in code** as strategy components keyed by `RuleType`.

- `FraudRule` (id, name, type, threshold, weight, enabled) is a JPA entity persisted via Flyway seed data
- Each `FraudRule.RuleType` has a corresponding `FraudRuleEvaluator` Spring component implementing a common interface
- The `AssessFraudService` loads enabled rules, dispatches each to the evaluator for its type, and aggregates scores
- Adding a **new rule instance** (e.g. higher amount threshold) = insert a DB row, zero code
- Adding a **new rule type** (e.g. device fingerprint check) = one new `FraudRuleEvaluator` implementation + new `RuleType` enum value

## Alternatives Considered

### Alternative 1: Rules as YAML/Properties files
- **Pros**: Simple, versioned in git
- **Cons**: Requires redeployment to change rules; no per-environment dynamic tuning; hard to manage many rules

### Alternative 2: Full DSL rules engine (Drools)
- **Pros**: Powerful complex-event processing
- **Cons**: Heavy dependency, steep learning curve, overkill for the current rule set; expensive to host

### Alternative 3: Runtime rule configuration in DB
- **Pros**: Dynamic tuning without redeploy, auditable, meets extensibility goals
- **Cons**: Requires validation/migration discipline; seed data management via Flyway

**Why not chosen**: YAML/props lack runtime configurability; Drools is disproportionate complexity for 4 rule types. DB-backed rules with code-strategy evaluators balances flexibility and maintainability.

## Consequences

### Positive
- Fraud analysts can enable/disable rules and adjust weights without redeployment
- New rules of existing types are data-only changes
- Strategy pattern keeps rule logic isolated and unit-testable
- Risk scoring is deterministic and auditable

### Negative
- Rule config drift possible across environments without careful migration discipline
- Evaluator components must be registered for every RuleType (a missing evaluator silently skips the rule)

### Risks
- **Risk**: Rule weight inflation pushing all scores to REJECT — **Mitigation**: cap composite score at 100, document default weights
- **Risk**: Unconfigured rule type is silently ignored — **Mitigation**: log a warning when a rule has no matching evaluator
- **Risk**: Velocity rule lacks real transaction history — **Mitigation**: enrich `TransactionContext.recentTransactionsCount` from Payment Service (future) before async assessment

## Related Decisions

- ADR on Transactional Outbox (events) — FraudAssessmentCompleted published via Kafka
- ADR on UUIDv7 identifiers — FraudAssessment uses UUIDv7

## References

- `specs/008-fraud-detection/spec.md`
- `specs/008-fraud-detection/contracts/api/fraud-api.yaml`
- `specs/008-fraud-detection/contracts/events/fraud-assessment-completed.yaml`
