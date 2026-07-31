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
    participant Svc as AssessFraudService
    participant Rules as RuleRepository
    participant Scorer as RiskScorer
    participant Decision as DecisionMaker
    participant Repo as AssessmentRepository
    participant Event as EventPublisher

    Ctrl->>Svc: assess(command)
    Svc->>Rules: findEnabledRules()
    Rules-->>Svc: List<FraudRule>
    loop each enabled rule
        Svc->>Svc: dispatch to FraudRuleEvaluator
    end
    Svc->>Scorer: score(evaluations)
    Svc->>Decision: decide(riskScore)
    Svc->>Repo: save(FraudAssessment)
    Svc->>Event: publish(FraudAssessmentCompleted)
    Svc-->>Ctrl: FraudAssessment

    Ctrl->>Svc: findById(assessmentId)
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

## Behavior

1. Loads enabled rules from `FraudRuleRepository`
2. Evaluates each rule via the matching `FraudRuleEvaluator`
3. Aggregates scores with `RiskScorer` (0-100)
4. Applies thresholds with `DecisionMaker` (APPROVE < 30, REVIEW 30-70, REJECT > 70)
5. Persists `FraudAssessment`
6. Publishes [[03 - Domain Events/FraudAssessmentCompleted\|FraudAssessmentCompleted]]

## Implementation

- **Implemented by**: `AssessFraudService` in [[01 - Services/Fraud Service\|Fraud Service]]
- **Exposed by**: `FraudController` (`POST /api/v1/fraud/assess`, `GET /api/v1/fraud/assessments/{id}`)
