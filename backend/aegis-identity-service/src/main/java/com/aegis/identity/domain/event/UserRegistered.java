package com.aegis.identity.domain.event;

import com.aegis.common.util.UuidV7Generator;

import java.time.Instant;
import java.util.UUID;

public record UserRegistered(
        UUID eventId,
        String eventType,
        String schemaVersion,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Instant registeredAt,
        String correlationId
) {

    private static final String EVENT_TYPE = "USER_REGISTERED";
    private static final String SCHEMA_VERSION = "1.0";

    public UserRegistered(UUID userId, String email, String firstName,
                          String lastName, Instant registeredAt, String correlationId) {
        this(
                UuidV7Generator.generate(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                userId,
                email,
                firstName,
                lastName,
                registeredAt,
                correlationId
        );
    }
}
