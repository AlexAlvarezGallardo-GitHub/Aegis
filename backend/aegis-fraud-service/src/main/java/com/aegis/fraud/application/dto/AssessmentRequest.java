package com.aegis.fraud.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AssessmentRequest(
        @NotNull(message = "transactionId is required")
        UUID transactionId,

        @NotBlank(message = "transactionType is required")
        String transactionType,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        @Digits(integer = 15, fraction = 2, message = "amount has too many digits")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        String currency,

        UUID sourceWalletId,
        UUID destWalletId,

        @NotNull(message = "userId is required")
        UUID userId,

        String countryCode
) {}
