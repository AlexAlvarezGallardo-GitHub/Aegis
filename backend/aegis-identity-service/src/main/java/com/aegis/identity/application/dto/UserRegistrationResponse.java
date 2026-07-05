package com.aegis.identity.application.dto;

import java.time.Instant;
import java.util.UUID;

public record UserRegistrationResponse(
        UUID userId,
        String email,
        String status,
        Instant registeredAt
) {
}
