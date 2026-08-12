package com.aegis.wallet.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Web-layer request for debiting a hold for a payment.
 */
public record DebitHoldRequest(
        @NotNull(message = "paymentId is required")
        UUID paymentId,

        @NotNull(message = "holdId is required")
        UUID holdId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        @Digits(integer = 15, fraction = 2, message = "amount has too many digits")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        String currency
) {}
