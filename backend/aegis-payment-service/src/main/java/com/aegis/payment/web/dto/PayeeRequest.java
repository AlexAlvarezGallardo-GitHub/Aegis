package com.aegis.payment.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Web-layer request object for the payee within a payment request.
 */
public record PayeeRequest(
        @NotBlank(message = "payee name is required")
        String name,

        @NotBlank(message = "payee id is required")
        String id,

        @NotNull(message = "payee type is required")
        String type
) {}
