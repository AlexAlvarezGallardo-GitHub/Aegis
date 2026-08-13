package com.aegis.wallet.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Web-layer request object for crediting a wallet with a refund.
 */
public record CreditRefundRequest(
        @NotNull(message = "refundId is required")
        UUID refundId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        @Digits(integer = 15, fraction = 2, message = "amount has too many digits")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        String currency
) {}
