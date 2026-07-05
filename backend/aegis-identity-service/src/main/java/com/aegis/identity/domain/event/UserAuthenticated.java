package com.aegis.identity.domain.event;

import com.aegis.common.util.UuidV7Generator;

import java.time.Instant;
import java.util.UUID;

public record UserAuthenticated(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID userId,
        String email,
        Instant timestamp,
        boolean success,
        String failureReason,
        String correlationId
) {

    private static final String EVENT_TYPE = "USER_AUTHENTICATED";
    private static final String SCHEMA_VERSION = "1.0";

    public UserAuthenticated(UUID userId, String email, boolean success,
                             String failureReason, String correlationId) {
        this(
                UuidV7Generator.generate(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                userId,
                email,
                Instant.now(),
                success,
                failureReason,
                correlationId
        );
    }
}
