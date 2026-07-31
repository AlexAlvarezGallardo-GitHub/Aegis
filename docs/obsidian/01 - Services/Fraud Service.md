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

**Purpose**: Real-time fraud detection engine evaluating transactions against configurable rules with risk scoring and approve/review/reject decisions.

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
    Kafka["Kafka<br/>payment.*"] --> Consumer
    Client["Payment/Wallet Service"] -->|POST /api/v1/fraud/assess| Ctrl
    Repo --> DB[("PostgreSQL<br/>aegis_fraud")]
    Pub --> Out["Kafka<br/>fraud.assessment.completed"]
    style DB fill:#afa,stroke:#333
    style Kafka fill:#fdb,stroke:#333
    style Out fill:#fdb,stroke:#333
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
- **Models**: `FraudAssessment`, `FraudDecision` (enum), `FraudRule`, `RuleEvaluation`
- **Events**: [[03 - Domain Events/FraudAssessmentCompleted\|FraudAssessmentCompleted]]
- **Exceptions**: `AssessmentNotFoundException`
- **Inbound Ports**: [[04 - Ports/inbound/AssessFraudUseCase\|AssessFraudUseCase]]
- **Outbound Ports**: `FraudRuleRepository`, `FraudAssessmentRepository`, `EventPublisher`

### Application (`com.aegis.fraud.application`)
- **Services**: `AssessFraudService`, `RiskScorer`, `DecisionMaker`
- **Rules Engine**: `FraudRuleEvaluator` (interface), `VelocityRuleEvaluator`, `AmountThresholdRuleEvaluator`, `GeographicRuleEvaluator`, `TimeBasedRuleEvaluator`
- **DTOs**: `AssessmentRequest`, `AssessmentResponse`

### Infrastructure (`com.aegis.fraud.infrastructure`)
- **Persistence**: `FraudRuleJpaEntity`, `FraudAssessmentJpaEntity`, repository adapters
- **Messaging**: `KafkaEventPublisher`, `TransactionEventConsumer`
- **Config**: `KafkaConfig`, `SecurityConfig`

### Web (`com.aegis.fraud.web`)
- **Controllers**: `FraudController`
- **Advice**: `FraudExceptionHandler`

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/fraud/assess` | Synchronous fraud assessment (score + decision) |
| GET | `/api/v1/fraud/assessments/{id}` | Retrieve stored assessment details |

## Business Rules

1. Risk score 0-100 (weighted rule aggregation, capped at 100)
2. Score < 30 → **APPROVE**
3. Score 30-70 → **REVIEW**
4. Score > 70 → **REJECT**
5. Rules are DB-configurable (see ADR-001 in `docs/adr/ADR-001-fraud-rules-configuration.md`)

## Events

| Direction | Event | Topic |
|-----------|-------|-------|
| Produced | [[03 - Domain Events/FraudAssessmentCompleted\|FraudAssessmentCompleted]] | `fraud.assessment.completed` |
| Consumed | TransferRequested | `payment.transfer.requested` |

## Dependencies

- **Depends on**: [[01 - Services/Common Module\|Common Module]], PostgreSQL, Kafka
- **Consumed by**: [[01 - Services/Audit Service\|Audit Service]] (via `fraud.assessment.completed`)

## Flyway Migrations

| File | Description |
|------|-------------|
| `V1__create_fraud_tables.sql` | Fraud rules + assessments (with seeded default rules) |
