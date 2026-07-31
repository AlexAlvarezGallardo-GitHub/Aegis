# Tasks: UC-008 Fraud Detection

## Phase 1: Spec
- [ ] Write spec (spec.md, plan.md, tasks.md)
- [ ] OpenAPI contract
- [ ] Event schema (FraudAssessmentCompleted)

## Phase 2: Domain Layer
- [ ] FraudDecision enum
- [ ] FraudAssessment aggregate
- [ ] FraudRule entity
- [ ] RuleEvaluation value object
- [ ] FraudAssessmentCompleted event

## Phase 3: Ports
- [ ] AssessFraudUseCase inbound port
- [ ] FraudRuleRepository outbound port

## Phase 4: Application Layer
- [ ] Rules engine (velocity, amount, geographic, time)
- [ ] RiskScorer
- [ ] DecisionMaker
- [ ] DTOs + mappers
- [ ] AssessFraudService

## Phase 5: Infrastructure
- [ ] JPA persistence
- [ ] Kafka producer
- [ ] Kafka consumer
- [ ] KafkaConfig

## Phase 6: Web
- [ ] FraudController (POST /assess, GET /assessments/{id})
- [ ] Exception handler

## Phase 7: Audit
- [ ] Audit consumer for FraudAssessmentCompleted

## Phase 8: Tests
- [ ] Rules engine tests
- [ ] Risk scorer tests
- [ ] Decision tests
- [ ] Service tests
- [ ] Controller tests

## Phase 9: Docs
- [ ] Obsidian vault (service, event, port, spec)
- [ ] ADR for rules configuration
