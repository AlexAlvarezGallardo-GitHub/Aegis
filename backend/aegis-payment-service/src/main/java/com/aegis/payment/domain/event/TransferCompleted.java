package com.aegis.payment.domain.event;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.payment.domain.model.Transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a transfer completes successfully.
 */
public record TransferCompleted(
        UUID eventId,
        String eventType,
        String schemaVersion,
        Instant occurredAt,
        UUID causationId,
        UUID correlationId,
        UUID aggregateId,
        String aggregateType,
        UUID transferId,
        UUID sourceWalletId,
        UUID destWalletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        Instant completedAt
) {

    private static final String EVENT_TYPE = "TRANSFER_COMPLETED";
    private static final String SCHEMA_VERSION = "1.0";
    private static final String AGGREGATE_TYPE = "TRANSFER";

    /**
     * Convenience constructor that derives envelope fields from the aggregate.
     *
     * @param transfer the completed transfer
     */
    public TransferCompleted(Transfer transfer) {
        this(
                UuidV7Generator.generate(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                Instant.now(),
                transfer.getId(),
                transfer.getId(),
                transfer.getId(),
                AGGREGATE_TYPE,
                transfer.getId(),
                transfer.getSourceWalletId(),
                transfer.getDestWalletId(),
                transfer.getUserId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getCompletedAt()
        );
    }
}
