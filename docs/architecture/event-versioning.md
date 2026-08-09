# Event Versioning and Standard Envelope

> **Status:** Schema contracts defined; `eventId`, `eventType`, `schemaVersion`
> and `correlationId` are present in all events. `occurredAt`/`causationId` are
> documented and added incrementally to new events.

## Standard event envelope

Every domain event published on Kafka carries the following envelope fields.
They are documented in the YAML contracts under `specs/*/contracts/events/`.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `eventId` | UUID | yes | Globally unique event identifier (used for deduplication) |
| `eventType` | string | yes | Type discriminator (e.g. `FUNDS_DEPOSITED`) |
| `schemaVersion` | string | yes | Semantic version of the payload schema (`"1.0"`) |
| `occurredAt` | string (date-time) | yes | When the event occurred in the producer (alias `timestamp`) |
| `correlationId` | string | yes | Correlation ID propagated across services |
| `causationId` | UUID | optional | The event/command that caused this event (chaining) |
| `aggregateId` | UUID | yes | The aggregate the event refers to (e.g. walletId, userId) |
| `aggregateType` | string | yes | Type of aggregate (`WALLET`, `USER`, `FRAUD_ASSESSMENT`, ...) |

### Compatibility with existing payloads

Existing events keep their business fields at the top level (not nested under
`payload`), which is the current on-the-wire format. The envelope is expressed as
conventions over the existing top-level fields:

- `FundsDeposited` → `eventId`, `eventType`, `schemaVersion`, `timestamp`
  (occurredAt alias), `correlationId`, `walletId` (aggregateId).
- `FraudAssessmentCompleted` → `eventId`, `eventType`, `schemaVersion`,
  `timestamp`, `assessmentId`/`transactionId` (aggregate context).
- `UserRegistered` → `eventId`, `eventType`, `schemaVersion`, `registeredAt`,
  `correlationId`, `userId` (aggregateId).

New events MUST include `causationId` and `occurredAt` going forward.

## Versioning strategy

- Payloads are versioned via `schemaVersion` (semver, `MAJOR.MINOR`).
- **Backward compatible changes** (additive fields, relaxed constraints) bump the
  minor: `1.0` → `1.1`. Existing consumers keep working.
- **Breaking changes** (field removal, type change, renames) bump the major:
  `1.0` → `2.0`, and require a coordinated producer/consumer rollout.
- Consumers deserialize with `JsonDeserializer` configured to tolerate unknown
  properties (forward compatibility for additive fields).

## Backward compatibility rules

1. Never remove a field without a major version bump.
2. Never change a field's type or semantics within the same major version.
3. New optional fields are safe (consumers ignore unknown fields).
4. New required fields are treated as a breaking change.

## Schema contracts

- `specs/004-deposit-funds/contracts/events/funds-deposited.yaml`
- `specs/008-fraud-detection/contracts/events/fraud-assessment-completed.yaml`
- Identity event contracts under `specs/001-user-registration/contracts/events/`

## See also

- [Retries and Dead Letter Topics](retry-dlt.md)
- [Idempotency](idempotency.md)
- [Kafka Topics](../obsidian/05%20-%20Infrastructure/Kafka%20Topics.md)
