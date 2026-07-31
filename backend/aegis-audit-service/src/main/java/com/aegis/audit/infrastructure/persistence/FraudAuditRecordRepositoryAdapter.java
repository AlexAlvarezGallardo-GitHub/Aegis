package com.aegis.audit.infrastructure.persistence;

import com.aegis.audit.domain.model.FraudAuditRecord;
import com.aegis.audit.domain.port.outbound.FraudAuditRecordRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter that bridges the domain {@link FraudAuditRecordRepository} port
 * with the Spring Data JPA {@link FraudAuditRecordJpaRepository}.
 */
@Repository
public class FraudAuditRecordRepositoryAdapter implements FraudAuditRecordRepository {

    private final FraudAuditRecordJpaRepository jpaRepository;

    public FraudAuditRecordRepositoryAdapter(FraudAuditRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public FraudAuditRecord save(FraudAuditRecord record) {
        FraudAuditRecordJpaEntity entity = toEntity(record);
        FraudAuditRecordJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private FraudAuditRecordJpaEntity toEntity(FraudAuditRecord record) {
        return new FraudAuditRecordJpaEntity(
                record.id(),
                record.assessmentId(),
                record.transactionId(),
                record.transactionType(),
                record.riskScore(),
                record.decision(),
                record.rulesEvaluated(),
                record.eventTimestamp(),
                record.ingestedAt()
        );
    }

    private FraudAuditRecord toDomain(FraudAuditRecordJpaEntity entity) {
        return new FraudAuditRecord(
                entity.getId(),
                entity.getAssessmentId(),
                entity.getTransactionId(),
                entity.getTransactionType(),
                entity.getRiskScore(),
                entity.getDecision(),
                entity.getRulesEvaluated(),
                entity.getEventTimestamp(),
                entity.getIngestedAt()
        );
    }
}
