package com.aegis.reporting.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing a funds deposit into a wallet.
 * Consumed from the Kafka topic {@code wallet.funds.deposited}.
 *
 * @param eventId       unique identifier of the event
 * @param eventType     type discriminator (always {@code FUNDS_DEPOSITED})
 * @param schemaVersion schema version of the event payload
 * @param walletId      identifier of the target wallet
 * @param userId        identifier of the wallet owner
 * @param amount        amount deposited
 * @param currency      ISO-4217 currency code
 * @param source        origin of the deposit (e.g. {@code BANK_TRANSFER})
 * @param reference     external transaction reference
 * @param newBalance    wallet balance after the deposit
 * @param timestamp     instant when the deposit occurred
 * @param correlationId correlation identifier for distributed tracing
 */
public record FundsDepositedEvent(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String source,
        String reference,
        BigDecimal newBalance,
        Instant timestamp,
        String correlationId
) {
}
