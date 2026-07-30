package com.aegis.wallet.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStatusCommand(
        @NotBlank(message = "Status is required")
        String status
) {}
