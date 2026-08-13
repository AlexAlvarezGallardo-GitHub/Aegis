package com.aegis.bff.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body for refunding a completed payment.
 *
 * <p>The {@code userId} is intentionally excluded: the BFF derives it from the
 * session JWT to prevent impersonation.</p>
 *
 * @param amount    the refund amount (optional; defaults to the full payment amount)
 * @param reason    an optional refund reason
 * @param reference the idempotency / external reference
 */
public record CreateRefundRequest(
        @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @Size(max = 255) String reason,
        @NotBlank String reference) {
}
