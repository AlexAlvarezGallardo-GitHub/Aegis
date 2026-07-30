package com.aegis.wallet.domain.event;

import com.aegis.common.util.UuidV7Generator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletBalanceAdjusted(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID walletId,
        UUID userId,
        BigDecimal previousBalance,
        BigDecimal newBalance,
        BigDecimal amount,
        String currency,
        String description,
        Instant timestamp,
        String correlationId
) {

    private static final String EVENT_TYPE = "WALLET_BALANCE_ADJUSTED";
    private static final String SCHEMA_VERSION = "1.0";

    public WalletBalanceAdjusted(UUID walletId, UUID userId, BigDecimal previousBalance,
                                  BigDecimal newBalance, BigDecimal amount, String currency,
                                  String description, String correlationId) {
        this(
                UuidV7Generator.generate(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                walletId,
                userId,
                previousBalance,
                newBalance,
                amount,
                currency,
                description,
                Instant.now(),
                correlationId
        );
    }
}
