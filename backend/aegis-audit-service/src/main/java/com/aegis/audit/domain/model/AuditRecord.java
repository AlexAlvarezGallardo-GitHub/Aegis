package com.aegis.audit.domain.model;

import com.aegis.common.util.UuidV7Generator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing an audit record for a financial transaction.
 *
 * <p>Each record corresponds to a financial event ingested from Kafka,
 * providing a durable audit trail for regulatory compliance.</p>
 *
 * @param id              unique identifier (UUIDv7)
 * @param walletId        wallet identifier
 * @param userId          user identifier
 * @param amount          amount deposited
 * @param currency        ISO 4217 currency code
 * @param source          source of deposit (e.g., BANK_TRANSFER)
 * @param reference       transaction reference
 * @param newBalance      new balance after deposit
 * @param eventTimestamp  timestamp of the original event
 * @param ingestedAt      timestamp when the record was ingested
 * @param correlationId   correlation ID for distributed tracing
 */
public record AuditRecord(
        UUID id,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String source,
        String reference,
        BigDecimal newBalance,
        Instant eventTimestamp,
        Instant ingestedAt,
        String correlationId
) {

    /**
     * Factory method that creates a new AuditRecord with a generated UUIDv7 identifier.
     *
     * @param walletId        wallet identifier
     * @param userId          user identifier
     * @param amount          amount deposited
     * @param currency        ISO 4217 currency code
     * @param source          source of deposit
     * @param reference       transaction reference
     * @param newBalance      new balance after deposit
     * @param eventTimestamp  timestamp of the original event
     * @param ingestedAt      timestamp when the record was ingested
     * @param correlationId   correlation ID for distributed tracing
     * @return a new AuditRecord instance
     */
    public static AuditRecord create(
            UUID walletId,
            UUID userId,
            BigDecimal amount,
            String currency,
            String source,
            String reference,
            BigDecimal newBalance,
            Instant eventTimestamp,
            Instant ingestedAt,
            String correlationId
    ) {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(newBalance, "newBalance must not be null");
        Objects.requireNonNull(eventTimestamp, "eventTimestamp must not be null");
        Objects.requireNonNull(ingestedAt, "ingestedAt must not be null");

        return new AuditRecord(
                UuidV7Generator.generate(),
                walletId,
                userId,
                amount,
                currency,
                source,
                reference,
                newBalance,
                eventTimestamp,
                ingestedAt,
                correlationId
        );
    }
}
