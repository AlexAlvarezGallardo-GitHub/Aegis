---
type: port
service: aegis-fraud-service
layer: domain
tags: [port, inbound, use-case, fraud]
status: implemented
port-type: inbound
---

# AssessFraudUseCase

Inbound port (interface) for fraud assessment.

```mermaid
sequenceDiagram
    participant Ctrl as FraudController
    participant Port as AssessFraudUseCase (port)
    participant Svc as AssessFraudService (impl)
    participant Rules as FraudRuleRepository
    participant Evaluator as FraudRuleEvaluator
    participant Scorer as RiskScorer
    participant Decision as DecisionMaker
    participant Repo as FraudAssessmentRepository
    participant Event as EventPublisher

    Ctrl->>Port: assess(command)
    Port->>Svc: delegate
    Svc->>Rules: findEnabledRules()
    Rules-->>Svc: List<FraudRule>
    loop each enabled rule with evaluator
        Svc->>Evaluator: evaluate(rule, context)
        Evaluator-->>Svc: RuleEvaluation
    end
    Svc->>Scorer: score(evaluations)
    Scorer-->>Svc: riskScore (0-100)
    Svc->>Decision: decide(riskScore)
    alt riskScore < 30
        Decision-->>Svc: APPROVE
    else riskScore > 70
        Decision-->>Svc: REJECT
    else
        Decision-->>Svc: REVIEW
    end
    Svc->>Svc: FraudAssessment.complete(...)
    Svc->>Repo: save(assessment)
    Svc->>Event: publish(FraudAssessmentCompleted)
    Svc-->>Ctrl: FraudAssessment

    Ctrl->>Port: findById(assessmentId)
    alt exists
        Repo-->>Svc: FraudAssessment
    else missing
        Repo-->>Svc: empty → throw AssessmentNotFoundException
    end
```

## Methods

```java
FraudAssessment assess(AssessmentCommand command);
FraudAssessment findById(UUID assessmentId);
```

## Command

```java
record AssessmentCommand(UUID transactionId, String transactionType, BigDecimal amount,
                         String currency, UUID sourceWalletId, UUID destWalletId,
                         UUID userId, String countryCode) {}
```

## Behavior

1. Loads enabled rules from [[04 - Ports/outbound/FraudRuleRepository|FraudRuleRepository]]
2. Evaluates each rule via the matching `FraudRuleEvaluator` by [[02 - Domain Models/FraudRule|RuleType]]
3. Aggregates scores with `RiskScorer` (0-100)
4. Applies thresholds with `DecisionMaker` (APPROVE < 30, REVIEW 30-70, REJECT > 70)
5. Persists [[02 - Domain Models/FraudAssessment|FraudAssessment]] via [[04 - Ports/outbound/FraudAssessmentRepository|FraudAssessmentRepository]]
6. Publishes [[03 - Domain Events/FraudAssessmentCompleted|FraudAssessmentCompleted]] via [[04 - Ports/outbound/EventPublisher|EventPublisher]]

## Implementation

- **Implemented by**: `AssessFraudService` in [[01 - Services/Fraud Service|Fraud Service]]
- **Exposed by**: `FraudController` (`POST /api/v1/fraud/assess`, `GET /api/v1/fraud/assessments/{id}`)
