---
type: service
service: aegis-fraud-service
layer: all
tags: [ddd, hexagonal, java, spring, fraud, risk]
status: implemented
port: 8089
database: aegis_fraud
---

# Fraud Service

**Purpose**: Rule-based fraud detection engine (VELOCITY, AMOUNT, GEOGRAPHIC, TIME) that computes a risk score (0-100) and an APPROVE/REVIEW/REJECT decision. It consumes `payment.transfer.requested` and evaluates transactions via REST.

```mermaid
graph TB
    subgraph Hexagonal["Hexagonal Architecture"]
        direction TB
        Ctrl["FraudController"]
        Consumer["TransactionEventConsumer"]
        Svc["AssessFraudService"]
        Rules["Rules Engine<br/>Velocity / Amount / Geographic / Time"]
        Scorer["RiskScorer"]
        Decision["DecisionMaker"]
        Repo["FraudAssessmentRepository"]
        Pub["KafkaEventPublisher"]
        Ctrl --> Svc
        Consumer --> Svc
        Svc --> Rules
        Svc --> Scorer
        Svc --> Decision
        Svc --> Repo
        Svc --> Pub
    end
    Kafka["Kafka<br/>payment.transfer.requested"] --> Consumer
    Client["Payment/Wallet Service"] -->|POST /api/v1/fraud/assess| Ctrl
    Repo --> DB[("PostgreSQL<br/>aegis_fraud")]
    Pub --> Out["Kafka<br/>fraud.assessment.completed"]
    style Ctrl fill:#bbf,stroke:#333,color:#000
    style Consumer fill:#bbf,stroke:#333,color:#000
    style Svc fill:#bbf,stroke:#333,color:#000
    style Rules fill:#bbf,stroke:#333,color:#000
    style Scorer fill:#bbf,stroke:#333,color:#000
    style Decision fill:#bbf,stroke:#333,color:#000
    style Client fill:#bbf,stroke:#333,color:#000
    style Repo fill:#fdb,stroke:#333,color:#000
    style Pub fill:#fdb,stroke:#333,color:#000
    style Kafka fill:#fdb,stroke:#333,color:#000
    style Out fill:#fdb,stroke:#333,color:#000
    style DB fill:#afa,stroke:#333,color:#000
```

```mermaid
sequenceDiagram
    participant Client as Payment Service
    participant Ctrl as FraudController
    participant Svc as AssessFraudService
    participant Rules as Rules Engine
    participant Scorer as RiskScorer
    participant Decision as DecisionMaker
    participant DB as PostgreSQL
    participant Kafka as Kafka

    Client->>Ctrl: POST /api/v1/fraud/assess
    Ctrl->>Svc: assess(command)
    Svc->>DB: load enabled rules
    Svc->>Rules: evaluate each rule
    Rules-->>Svc: List<RuleEvaluation>
    Svc->>Scorer: score(evaluations)
    Scorer-->>Svc: riskScore (0-100)
    Svc->>Decision: decide(riskScore)
    Decision-->>Svc: APPROVE / REVIEW / REJECT
    Svc->>DB: save FraudAssessment
    Svc->>Kafka: publish FraudAssessmentCompleted
    Svc-->>Ctrl: AssessmentResponse
    Ctrl-->>Client: 200 (decision)
```

## Hexagonal Structure

### Domain (`com.aegis.fraud.domain`)
- **Models**: `FraudAssessment`, `FraudDecision` (APPROVE, REVIEW, REJECT), `FraudRule` (`RuleType`: VELOCITY, AMOUNT, GEOGRAPHIC, TIME), `RuleEvaluation`
- **Events**: [[03 - Domain Events/FraudAssessmentCompleted\|FraudAssessmentCompleted]]
- **Exceptions**: `AssessmentNotFoundException`
- **Inbound Ports**: [[04 - Ports/inbound/AssessFraudUseCase\|AssessFraudUseCase]]
- **Outbound Ports**: `FraudRuleRepository`, `FraudAssessmentRepository`, [[04 - Ports/outbound/EventPublisher\|EventPublisher]]

### Application (`com.aegis.fraud.application`)
- **Services**: `AssessFraudService`, `RiskScorer`, `DecisionMaker`
- **Rules Engine**: `FraudRuleEvaluator` (interface), `VelocityRuleEvaluator`, `AmountThresholdRuleEvaluator`, `GeographicRuleEvaluator`, `TimeBasedRuleEvaluator`, `TransactionContext`
- **DTOs**: `AssessmentRequest`, `AssessmentResponse`

### Infrastructure (`com.aegis.fraud.infrastructure`)
- **Persistence**: `FraudRuleJpaEntity`, `FraudAssessmentJpaEntity`, `ProcessedEventJpaRepository`, repository adapters
- **Messaging**: `KafkaEventPublisher`, `TransactionEventConsumer`
- **Config**: `KafkaConfig`, `SecurityConfig`

### Web (`com.aegis.fraud.web`)
- **Controllers**: `FraudController`
- **Advice**: `FraudExceptionHandler`

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/fraud/assess` | Synchronous fraud assessment (score + decision) |
| GET | `/api/v1/fraud/assessments/{assessmentId}` | Retrieve stored assessment details |

## Business Rules

1. Risk score 0-100 (weighted rule aggregation, capped at 100)
2. Score < `review-threshold` (30) → **APPROVE**
3. Score > `reject-threshold` (70) → **REJECT**
4. Otherwise → **REVIEW**
5. Thresholds are configurable (`aegis.fraud.review-threshold`, `aegis.fraud.reject-threshold`)
6. Rules are DB-configurable and seeded at startup (see ADR-001 in `docs/adr/ADR-001-fraud-rules-configuration.md`)

## Events

| Direction | Event | Topic |
|-----------|-------|-------|
| Produced | [[03 - Domain Events/FraudAssessmentCompleted\|FraudAssessmentCompleted]] | `fraud.assessment.completed` |
| Consumed | `TransferRequested` | `payment.transfer.requested` |

## Dependencies

- **Depends on**: [[01 - Services/Common Module\|Common Module]], PostgreSQL, Kafka
- **Consumed by**: [[01 - Services/Audit Service\|Audit Service]] (via `fraud.assessment.completed`)

## Flyway Migrations

| File | Description |
|------|-------------|
| `V1__create_fraud_tables.sql` | Fraud rules + assessments (seeded with 4 default rules) |
| `V2__create_outbox_events.sql` | Outbox table |
| `V3__create_processed_events_table.sql` | Processed events (idempotency) |
