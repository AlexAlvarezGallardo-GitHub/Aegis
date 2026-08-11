package com.aegis.reporting.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing a failed transfer.
 * <p>
 * Consumed from the Kafka topic {@code payment.transfer.failed}.
 * </p>
 *
 * @param eventId         unique identifier of the event
 * @param eventType       type discriminator (always {@code TRANSFER_FAILED})
 * @param schemaVersion   schema version of the event payload
 * @param transferId      identifier of the transfer
 * @param sourceWalletId  identifier of the source wallet
 * @param destWalletId    identifier of the destination wallet
 * @param userId          identifier of the user who initiated the transfer
 * @param amount          transfer amount
 * @param currency        ISO 4217 currency code
 * @param reference       transfer reference
 * @param timestamp       instant when the event occurred
 * @param correlationId   correlation identifier for distributed tracing
 * @param failureReason   reason the transfer failed
 * @param failureDetails  optional details about the failure
 * @param compensated     whether compensating actions were executed
 */
public record TransferFailedEvent(
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
        String correlationId,
        String failureReason,
        String failureDetails,
        boolean compensated
) {
}
