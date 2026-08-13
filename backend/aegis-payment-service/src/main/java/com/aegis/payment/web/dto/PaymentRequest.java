package com.aegis.payment.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Web-layer request object for creating a payment. Contains only validation annotations.
 * The authenticated user id is supplied via the X-User-Id header, not the body.
 */
public record PaymentRequest(
        @NotNull(message = "walletId is required")
        UUID walletId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        @Digits(integer = 15, fraction = 2, message = "amount has too many digits")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        String currency,

        @NotNull(message = "payee is required")
        PayeeRequest payee,

        String description,

        @NotBlank(message = "reference is required")
        String reference
) {}
