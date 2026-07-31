# ADR-002: Kafka Topic Configuration Strategy

## Status

Accepted

## Date

2026-07-31

## Context

Topic names were hardcoded in code in two places:
- `OutboxRelayScheduler.TOPIC_MAP` (static map of event type → topic) in identity and wallet services
- `@KafkaListener(topics = "...")` annotations in reporting, audit, and fraud consumers

Hardcoding topic names in code is brittle: renaming a topic or wiring a new event type requires a code change and redeploy, and producers/consumers can drift out of sync.

## Decision

**Topic names live in configuration** (`application.yml` under `aegis.kafka.topics`) and are referenced everywhere:

- **Outbox relay (producers)**: a `KafkaTopicsProperties` `@ConfigurationProperties` class binds the `aegis.kafka.topics` map (event type → topic). `OutboxRelayScheduler` resolves the topic via `topicFor(eventType)` at runtime.
- **Consumers**: `@KafkaListener(topics = "${aegis.kafka.topics.<key>}")` resolves the topic from the same configuration block.
- **Producers** (fraud service): `@Value("${aegis.kafka.topics.<key>}")` reads the configured topic.

Each service defines only the topics it produces/consumes, keeping config ownership local (hexagonal principle II: domain ownership).

## Alternatives Considered

### Alternative 1: Shared event schema registry / Confluent Schema Registry
- **Pros**: Full schema governance, centralized topic catalog
- **Cons**: Heavy infrastructure; unnecessary until the platform scales

### Alternative 2: Centralized topic constants in common module
- **Pros**: Single source of truth for topic names
- **Cons**: Couples all services to one module; topic renames still need redeploys; violates service config ownership

### Alternative 3: Configuration-driven topics (chosen)
- **Pros**: Rename/wire topics without code changes; per-service config; aligns with 12-factor config
- **Cons**: Topic names can drift if a producer/consumer config is mis-typed; mitigated by integration smoke tests

## Consequences

### Positive
- Topics are manageable at deployment time (env-specific overrides)
- Adding a new event type only requires a config entry, not code
- Producers and consumers reference the same config block

### Negative
- No compile-time check that a topic key exists — a typo surfaces at runtime (logged by relay as "No topic configured")

### Risks
- **Risk**: Config typo silently drops events — **Mitigation**: relay logs a warning when `topicFor` returns null and keeps the event PENDING-aware (currently marks PUBLISHED; a follow-up should keep it PENDING on missing mapping)

## Related Decisions

- ADR on Transactional Outbox (events published via outbox relay)
- ADR-001: fraud rules configuration strategy (same principle: configuration over hardcoding)

## References

- `specs/008-fraud-detection/contracts/events/fraud-assessment-completed.yaml`
- `specs/004-deposit-funds/contracts/events/funds-deposited.yaml`
