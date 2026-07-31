# Implementation Plan: UC-008 Fraud Detection

**Branch**: `feature/008-fraud-detection` | **Date**: 2026-07-31

## Implementation Order

```
1. Spec + contracts
2. Scaffold aegis-fraud-service module
3. Domain layer
   ├── FraudDecision (enum)
   ├── FraudAssessment (aggregate)
   ├── FraudRule (configurable rule)
   └── RuleEvaluation (value object)
4. Ports
   ├── AssessFraudUseCase (inbound)
   └── FraudRuleRepository (outbound)
5. Rules engine (application/service)
   ├── VelocityRuleEvaluator
   ├── AmountThresholdRuleEvaluator
   ├── GeographicRuleEvaluator
   └── TimeBasedRuleEvaluator
   └── RiskScorer (weighted aggregation)
   └── DecisionMaker (thresholds)
6. Application
   ├── DTOs (AssessmentRequest, AssessmentResponse)
   ├── Mapper
   └── AssessFraudService
7. Infrastructure
   ├── JPA entities + repositories
   ├── Kafka producer (fraud.assessment.completed)
   └── Kafka consumer (payment.*)
8. Web
   └── FraudController
9. Audit service consumer
10. Tests
11. Obsidian vault + ADR
```

## Key Decisions

| Decision | Choice |
|----------|--------|
| Rule storage | Database (FraudRule entity, configurable) |
| Scoring | Weighted sum of rule scores capped at 100 |
| Decision | APPROVE < 30, REVIEW 30-70, REJECT > 70 |
| Assessment endpoint | POST /api/v1/fraud/assess (sync, < 200ms) |
| Event publish | FraudAssessmentCompleted on fraud.assessment.completed |
