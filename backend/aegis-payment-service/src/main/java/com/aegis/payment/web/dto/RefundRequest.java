package com.aegis.payment.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Web-layer request object for refunding a payment.
 */
public record RefundRequest(
        @DecimalMin(value = "0.01", message = "amount must be positive")
        @Digits(integer = 15, fraction = 2, message = "amount has too many digits")
        BigDecimal amount,

        @Size(max = 255, message = "reason must not exceed 255 characters")
        String reason,

        @NotBlank(message = "reference is required")
        @Size(max = 255, message = "reference must not exceed 255 characters")
        String reference
) {}
