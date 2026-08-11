package com.aegis.wallet.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Web-layer request body for creating a funds hold.
 *
 * @param amount    the reserved amount (must be positive)
 * @param currency  the 3-letter ISO 4217 currency code
 * @param reference the transfer id (idempotency key)
 */
public record HoldRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotBlank(message = "Reference is required")
        String reference
) {
}
