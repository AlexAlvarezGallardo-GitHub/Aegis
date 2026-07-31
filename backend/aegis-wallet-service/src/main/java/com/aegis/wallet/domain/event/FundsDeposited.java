package com.aegis.wallet.domain.event;

import com.aegis.common.util.UuidV7Generator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FundsDeposited(
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

    private static final String EVENT_TYPE = "FUNDS_DEPOSITED";
    private static final String SCHEMA_VERSION = "1.0";

    public FundsDeposited(UUID walletId, UUID userId, BigDecimal amount, String currency,
                          String source, String reference, BigDecimal newBalance, String correlationId) {
        this(
                UuidV7Generator.generate(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                walletId,
                userId,
                amount,
                currency,
                source,
                reference,
                newBalance,
                Instant.now(),
                correlationId
        );
    }
}
