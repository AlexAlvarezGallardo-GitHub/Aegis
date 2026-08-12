package com.aegis.payment.domain.event;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.payment.domain.model.Payee;
import com.aegis.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a payment completes successfully.
 */
public record PaymentExecuted(
        UUID eventId,
        String eventType,
        String schemaVersion,
        Instant occurredAt,
        UUID causationId,
        UUID correlationId,
        UUID aggregateId,
        String aggregateType,
        UUID paymentId,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        Payee payee,
        String description,
        String reference,
        UUID fraudAssessmentId,
        UUID holdId,
        Instant completedAt
) {

    private static final String EVENT_TYPE = "PAYMENT_EXECUTED";
    private static final String SCHEMA_VERSION = "1.0";
    private static final String AGGREGATE_TYPE = "PAYMENT";

    /**
     * Convenience constructor that derives envelope fields from the aggregate.
     *
     * @param payment the completed payment
     */
    public PaymentExecuted(Payment payment) {
        this(
                UuidV7Generator.generate(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                Instant.now(),
                payment.getId(),
                payment.getId(),
                payment.getId(),
                AGGREGATE_TYPE,
                payment.getId(),
                payment.getWalletId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPayee(),
                payment.getDescription(),
                payment.getReference(),
                payment.getFraudAssessmentId(),
                payment.getHoldId(),
                payment.getCompletedAt()
        );
    }
}
