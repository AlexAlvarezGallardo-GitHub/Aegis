package com.aegis.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "User registration response")
public record UserRegistrationResponse(
        @Schema(description = "Unique identifier for the new user (UUID v7)", example = "01912345-6789-7abc-def0-123456789abc")
        UUID userId,

        @Schema(description = "The registered email address", example = "john.doe@example.com")
        String email,

        @Schema(description = "Current account status", example = "PENDING_VERIFICATION",
                allowableValues = {"PENDING_VERIFICATION", "ACTIVE", "LOCKED", "SUSPENDED"})
        String status,

        @Schema(description = "Registration timestamp (UTC)", example = "2026-06-28T14:30:00.000Z")
        Instant registeredAt
) {
}
