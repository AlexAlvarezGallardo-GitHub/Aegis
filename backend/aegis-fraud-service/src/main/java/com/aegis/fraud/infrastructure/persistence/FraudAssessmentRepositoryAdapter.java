package com.aegis.fraud.infrastructure.persistence;

import com.aegis.fraud.domain.model.FraudAssessment;
import com.aegis.fraud.domain.model.FraudDecision;
import com.aegis.fraud.domain.port.outbound.FraudAssessmentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class FraudAssessmentRepositoryAdapter implements FraudAssessmentRepository {

    private final FraudAssessmentJpaRepository jpaRepository;

    public FraudAssessmentRepositoryAdapter(FraudAssessmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public FraudAssessment save(FraudAssessment assessment) {
        jpaRepository.save(new FraudAssessmentJpaEntity(
                assessment.getAssessmentId(),
                assessment.getTransactionId(),
                assessment.getTransactionType(),
                assessment.getRiskScore(),
                FraudAssessmentJpaEntity.FraudDecisionJpa.valueOf(assessment.getDecision().name()),
                assessment.getRulesEvaluated(),
                assessment.getTimestamp()));
        return assessment;
    }

    @Override
    public Optional<FraudAssessment> findById(UUID assessmentId) {
        return jpaRepository.findById(assessmentId)
                .map(entity -> FraudAssessment.rehydrate(
                        entity.getId(),
                        entity.getTransactionId(),
                        entity.getTransactionType(),
                        entity.getRiskScore(),
                        FraudDecision.valueOf(entity.getDecision().name()),
                        entity.toRuleEvaluations(),
                        entity.getTimestamp()));
    }
}
