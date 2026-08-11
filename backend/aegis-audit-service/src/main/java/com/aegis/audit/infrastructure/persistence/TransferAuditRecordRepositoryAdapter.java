package com.aegis.audit.infrastructure.persistence;

import com.aegis.audit.domain.model.TransferAuditRecord;
import com.aegis.audit.domain.port.outbound.TransferAuditRecordRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter that bridges the domain {@link TransferAuditRecordRepository} port
 * with the Spring Data JPA {@link TransferAuditRecordJpaRepository}.
 */
@Repository
public class TransferAuditRecordRepositoryAdapter implements TransferAuditRecordRepository {

    private final TransferAuditRecordJpaRepository jpaRepository;

    public TransferAuditRecordRepositoryAdapter(TransferAuditRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TransferAuditRecord save(TransferAuditRecord record) {
        TransferAuditRecordJpaEntity entity = toEntity(record);
        TransferAuditRecordJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private TransferAuditRecordJpaEntity toEntity(TransferAuditRecord record) {
        return new TransferAuditRecordJpaEntity(
                record.id(),
                record.eventId(),
                record.transferId(),
                record.eventType(),
                record.sourceWalletId(),
                record.destWalletId(),
                record.userId(),
                record.amount(),
                record.currency(),
                record.reference(),
                record.failureReason(),
                record.correlationId(),
                record.eventTimestamp(),
                record.ingestedAt()
        );
    }

    private TransferAuditRecord toDomain(TransferAuditRecordJpaEntity entity) {
        return new TransferAuditRecord(
                entity.getId(),
                entity.getEventId(),
                entity.getTransferId(),
                entity.getEventType(),
                entity.getSourceWalletId(),
                entity.getDestWalletId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getReference(),
                entity.getFailureReason(),
                entity.getCorrelationId(),
                entity.getEventTimestamp(),
                entity.getIngestedAt()
        );
    }
}
