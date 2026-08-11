package com.aegis.reporting.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing a completed transfer.
 * <p>
 * Consumed from the Kafka topic {@code payment.transfer.completed}.
 * </p>
 *
 * @param eventId            unique identifier of the event
 * @param eventType          type discriminator (always {@code TRANSFER_COMPLETED})
 * @param schemaVersion      schema version of the event payload
 * @param transferId         identifier of the transfer
 * @param sourceWalletId     identifier of the source wallet
 * @param destWalletId       identifier of the destination wallet
 * @param userId             identifier of the user who initiated the transfer
 * @param amount             transferred amount
 * @param currency           ISO 4217 currency code
 * @param reference          transfer reference
 * @param timestamp          instant when the event occurred
 * @param correlationId      correlation identifier for distributed tracing
 * @param fraudAssessmentId  identifier of the fraud assessment
 * @param holdId             identifier of the funds hold
 * @param sourceNewBalance   source wallet balance after the transfer
 * @param destNewBalance     destination wallet balance after the transfer
 */
public record TransferCompletedEvent(
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
        UUID fraudAssessmentId,
        UUID holdId,
        BigDecimal sourceNewBalance,
        BigDecimal destNewBalance
) {
}
