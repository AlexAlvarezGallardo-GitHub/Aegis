package com.aegis.audit.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing a funds deposit into a wallet.
 * <p>
 * This record is used for Kafka deserialization of the {@code wallet.funds.deposited} topic.
 * </p>
 *
 * @param eventId         Unique identifier of the event
 * @param eventType       Type of the event (FUNDS_DEPOSITED)
 * @param schemaVersion   Version of the event schema
 * @param walletId        Identifier of the wallet receiving the funds
 * @param userId          Identifier of the user owning the wallet
 * @param amount          Amount deposited
 * @param currency        ISO 4217 currency code
 * @param source          Source of the deposit (e.g., BANK_TRANSFER)
 * @param reference       Transaction reference
 * @param newBalance      New balance after the deposit
 * @param timestamp       Timestamp when the event occurred
 * @param correlationId   Correlation ID for distributed tracing
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
