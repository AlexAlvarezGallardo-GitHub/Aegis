package com.aegis.audit.infrastructure.persistence;

import com.aegis.audit.domain.model.RefundAuditRecord;
import com.aegis.audit.domain.port.outbound.RefundAuditRecordRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter that bridges the domain {@link RefundAuditRecordRepository} port
 * with the Spring Data JPA {@link RefundAuditRecordJpaRepository}.
 */
@Repository
public class RefundAuditRecordRepositoryAdapter implements RefundAuditRecordRepository {

    private final RefundAuditRecordJpaRepository jpaRepository;

    public RefundAuditRecordRepositoryAdapter(RefundAuditRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RefundAuditRecord save(RefundAuditRecord record) {
        RefundAuditRecordJpaEntity entity = toEntity(record);
        RefundAuditRecordJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private RefundAuditRecordJpaEntity toEntity(RefundAuditRecord record) {
        return new RefundAuditRecordJpaEntity(
                record.id(),
                record.eventId(),
                record.refundId(),
                record.paymentId(),
                record.walletId(),
                record.userId(),
                record.amount(),
                record.currency(),
                record.reason(),
                record.reference(),
                record.correlationId(),
                record.eventTimestamp(),
                record.ingestedAt()
        );
    }

    private RefundAuditRecord toDomain(RefundAuditRecordJpaEntity entity) {
        return new RefundAuditRecord(
                entity.getId(),
                entity.getEventId(),
                entity.getRefundId(),
                entity.getPaymentId(),
                entity.getWalletId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getReason(),
                entity.getReference(),
                entity.getCorrelationId(),
                entity.getEventTimestamp(),
                entity.getIngestedAt()
        );
    }
}
