package com.aegis.bff.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for initiating a payment to a payee.
 *
 * <p>The {@code userId} is intentionally excluded: the BFF derives it from the
 * session JWT to prevent impersonation.</p>
 *
 * @param walletId    the source wallet id
 * @param amount      the amount to pay
 * @param currency    the 3-letter ISO 4217 currency code
 * @param payee       the payee information
 * @param description an optional description
 * @param reference   the idempotency / external reference
 */
public record CreatePaymentRequest(
        @NotNull UUID walletId,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @NotNull @Size(min = 3, max = 3) String currency,
        @NotNull @Valid PayeeRequest payee,
        @Size(max = 255) String description,
        @NotNull String reference) {
}
