package com.aegis.payment.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Web-layer request object for creating a transfer. Contains only validation annotations.
 */
public record TransferRequest(
        @NotNull(message = "sourceWalletId is required")
        UUID sourceWalletId,

        @NotNull(message = "destWalletId is required")
        UUID destWalletId,

        @NotNull(message = "userId is required")
        UUID userId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        @Digits(integer = 15, fraction = 2, message = "amount has too many digits")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        String currency,

        String description,

        @NotBlank(message = "reference is required")
        String reference
) {}
