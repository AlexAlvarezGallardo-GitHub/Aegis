package com.aegis.wallet.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Web-layer request body for depositing funds into a wallet.
 *
 * @param amount    the deposit amount (must be positive)
 * @param currency  the 3-letter ISO 4217 currency code
 * @param source    the source of the funds
 * @param reference a unique reference for the deposit
 */
public record DepositFundsRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotBlank(message = "Source is required")
        String source,

        @NotBlank(message = "Reference is required")
        String reference
) {
}
