---
type: domain-event
service: aegis-fraud-service
layer: domain
tags: [event, kafka, fraud, risk]
status: implemented
topic: fraud.assessment.completed
---

# FraudAssessmentCompleted

Published when a fraud assessment finishes for a transaction.

```mermaid
graph LR
    Fraud[Fraud Service] -->|publishes| Topic[fraud.assessment.completed]
    Topic --> Audit[Audit Service]
    style Fraud fill:#bbf,stroke:#333,color:#000
    style Topic fill:#fdb,stroke:#333,color:#000
    style Audit fill:#bfb,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant Fraud as Fraud Service
    participant Svc as AssessFraudService
    participant Pub as KafkaEventPublisher
    participant DB as PostgreSQL (Outbox)
    participant Kafka as Kafka Topic
    participant Audit as Audit Consumer

    Svc->>Pub: publish(FraudAssessmentCompleted)
    Pub->>DB: INSERT outbox_event (payload=FraudAssessmentCompleted JSON)
    DB-->>Kafka: OutboxRelayScheduler polls & sends
    Kafka->>Audit: Consume (group=audit-group) → persist FraudAuditRecord
```

## Schema

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Unique event identifier |
| `eventType` | String | `FRAUD_ASSESSMENT_COMPLETED` |
| `schemaVersion` | String | `1.0` |
| `assessmentId` | UUID | Assessment's ID |
| `transactionId` | UUID | Assessed transaction's ID |
| `transactionType` | String | TRANSFER, PAYMENT, etc. |
| `riskScore` | int | Composite risk score 0-100 |
| `decision` | FraudDecision | APPROVE, REVIEW, REJECT |
| `rulesEvaluated` | List\<RuleEvaluation\> | Per-rule scores and details |
| `timestamp` | Instant | Assessment time |

## Details

- **Producer**: [[01 - Services/Fraud Service|Fraud Service]] via [[04 - Ports/outbound/EventPublisher|EventPublisher]]
- **Topic**: `fraud.assessment.completed` ([[05 - Infrastructure/Kafka Topics|Kafka Topics]])
- **Trigger**: `POST /api/v1/fraud/assess`
- **Consumed by**: [[01 - Services/Audit Service|Audit Service]] (persists [[02 - Domain Models/FraudAuditRecord|FraudAuditRecord]])
