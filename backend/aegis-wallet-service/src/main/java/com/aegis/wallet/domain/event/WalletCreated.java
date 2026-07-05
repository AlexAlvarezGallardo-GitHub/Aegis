package com.aegis.wallet.domain.event;

import com.aegis.common.util.UuidV7Generator;

import java.time.Instant;
import java.util.UUID;

public record WalletCreated(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID walletId,
        UUID userId,
        String currency,
        Instant timestamp,
        String correlationId
) {

    private static final String EVENT_TYPE = "WALLET_CREATED";
    private static final String SCHEMA_VERSION = "1.0";

    public WalletCreated(UUID walletId, UUID userId, String currency,
                         Instant timestamp, String correlationId) {
        this(
                UuidV7Generator.generate(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                walletId,
                userId,
                currency,
                timestamp,
                correlationId
        );
    }
}
