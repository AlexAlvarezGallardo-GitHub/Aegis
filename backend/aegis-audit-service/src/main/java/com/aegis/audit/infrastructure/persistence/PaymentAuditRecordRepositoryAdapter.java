package com.aegis.audit.infrastructure.persistence;

import com.aegis.audit.domain.model.PaymentAuditRecord;
import com.aegis.audit.domain.port.outbound.PaymentAuditRecordRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter that bridges the domain {@link PaymentAuditRecordRepository} port
 * with the Spring Data JPA {@link PaymentAuditRecordJpaRepository}.
 */
@Repository
public class PaymentAuditRecordRepositoryAdapter implements PaymentAuditRecordRepository {

    private final PaymentAuditRecordJpaRepository jpaRepository;

    public PaymentAuditRecordRepositoryAdapter(PaymentAuditRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PaymentAuditRecord save(PaymentAuditRecord record) {
        PaymentAuditRecordJpaEntity entity = toEntity(record);
        PaymentAuditRecordJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private PaymentAuditRecordJpaEntity toEntity(PaymentAuditRecord record) {
        return new PaymentAuditRecordJpaEntity(
                record.id(),
                record.eventId(),
                record.paymentId(),
                record.eventType(),
                record.walletId(),
                record.userId(),
                record.amount(),
                record.currency(),
                record.payeeName(),
                record.failureReason(),
                record.correlationId(),
                record.eventTimestamp(),
                record.ingestedAt()
        );
    }

    private PaymentAuditRecord toDomain(PaymentAuditRecordJpaEntity entity) {
        return new PaymentAuditRecord(
                entity.getId(),
                entity.getEventId(),
                entity.getPaymentId(),
                entity.getEventType(),
                entity.getWalletId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getPayeeName(),
                entity.getFailureReason(),
                entity.getCorrelationId(),
                entity.getEventTimestamp(),
                entity.getIngestedAt()
        );
    }
}
