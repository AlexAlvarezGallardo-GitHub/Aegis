package com.aegis.identity.domain.event;

import com.aegis.common.util.UuidV7Generator;

import java.time.Instant;
import java.util.UUID;

public record UserAccountLocked(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID userId,
        String email,
        Instant timestamp,
        int failureCount,
        String correlationId
) {

    private static final String EVENT_TYPE = "USER_ACCOUNT_LOCKED";
    private static final String SCHEMA_VERSION = "1.0";

    public UserAccountLocked(UUID userId, String email, int failureCount, String correlationId) {
        this(
                UuidV7Generator.generate(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                userId,
                email,
                Instant.now(),
                failureCount,
                correlationId
        );
    }
}
