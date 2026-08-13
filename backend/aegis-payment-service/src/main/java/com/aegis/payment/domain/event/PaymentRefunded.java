package com.aegis.payment.domain.event;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.payment.domain.model.Refund;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a refund completes successfully.
 *
 * <p>Follows the standard envelope (ADR-009) with aggregateType=REFUND.</p>
 */
public record PaymentRefunded(
        UUID eventId,
        String eventType,
        String schemaVersion,
        Instant occurredAt,
        UUID causationId,
        UUID correlationId,
        UUID aggregateId,
        String aggregateType,
        UUID refundId,
        UUID paymentId,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String reason,
        String reference,
        BigDecimal newBalance,
        Instant timestamp
) {

    private static final String EVENT_TYPE = "PAYMENT_REFUNDED";
    private static final String SCHEMA_VERSION = "1.0";
    private static final String AGGREGATE_TYPE = "REFUND";

    /**
     * Convenience constructor that derives envelope fields from the aggregate.
     *
     * @param refund     the completed refund
     * @param newBalance the wallet balance after the refund credit
     */
    public PaymentRefunded(Refund refund, BigDecimal newBalance) {
        this(
                UuidV7Generator.generate(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                Instant.now(),
                refund.getPaymentId(),
                refund.getId(),
                refund.getId(),
                AGGREGATE_TYPE,
                refund.getId(),
                refund.getPaymentId(),
                refund.getWalletId(),
                refund.getUserId(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getReason(),
                refund.getReference(),
                newBalance,
                Instant.now()
        );
    }
}
