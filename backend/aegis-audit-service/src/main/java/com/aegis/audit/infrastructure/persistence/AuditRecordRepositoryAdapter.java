package com.aegis.audit.infrastructure.persistence;

import com.aegis.audit.domain.model.AuditRecord;
import com.aegis.audit.domain.port.outbound.AuditRecordRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter that bridges the domain {@link AuditRecordRepository} port
 * with the Spring Data JPA {@link AuditRecordJpaRepository}.
 */
@Repository
public class AuditRecordRepositoryAdapter implements AuditRecordRepository {

    private final AuditRecordJpaRepository jpaRepository;

    public AuditRecordRepositoryAdapter(AuditRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuditRecord save(AuditRecord record) {
        AuditRecordJpaEntity entity = toEntity(record);
        AuditRecordJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private AuditRecordJpaEntity toEntity(AuditRecord record) {
        return new AuditRecordJpaEntity(
                record.id(),
                record.walletId(),
                record.userId(),
                record.amount(),
                record.currency(),
                record.source(),
                record.reference(),
                record.newBalance(),
                record.eventTimestamp(),
                record.ingestedAt(),
                record.correlationId()
        );
    }

    private AuditRecord toDomain(AuditRecordJpaEntity entity) {
        return new AuditRecord(
                entity.getId(),
                entity.getWalletId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getSource(),
                entity.getReference(),
                entity.getNewBalance(),
                entity.getEventTimestamp(),
                entity.getIngestedAt(),
                entity.getCorrelationId()
        );
    }
}
