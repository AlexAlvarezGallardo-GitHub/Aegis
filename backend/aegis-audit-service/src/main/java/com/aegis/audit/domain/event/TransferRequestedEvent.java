package com.aegis.audit.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing a transfer request.
 * <p>
 * This record is used for Kafka deserialization of the {@code payment.transfer.requested} topic.
 * </p>
 *
 * @param eventId         unique identifier of the event
 * @param eventType       type of the event (TRANSFER_REQUESTED)
 * @param schemaVersion   version of the event schema
 * @param transferId      identifier of the transfer
 * @param sourceWalletId  identifier of the source wallet
 * @param destWalletId    identifier of the destination wallet
 * @param userId          identifier of the user initiating the transfer
 * @param amount          amount to transfer
 * @param currency        ISO 4217 currency code
 * @param reference       transfer reference
 * @param timestamp       timestamp when the event occurred
 * @param correlationId   correlation ID for distributed tracing
 */
public record TransferRequestedEvent(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID transferId,
        UUID sourceWalletId,
        UUID destWalletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String reference,
        Instant timestamp,
        String correlationId
) {
}
