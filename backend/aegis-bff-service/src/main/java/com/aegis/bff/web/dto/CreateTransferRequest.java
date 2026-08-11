package com.aegis.bff.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for initiating a transfer between two wallets.
 *
 * <p>The {@code userId} is intentionally excluded: the BFF derives it from the
 * session JWT to prevent impersonation.</p>
 *
 * @param sourceWalletId the source wallet id
 * @param destWalletId   the destination wallet id
 * @param amount         the amount to transfer
 * @param currency       the 3-letter ISO 4217 currency code
 * @param description    an optional description
 * @param reference      the idempotency / external reference
 */
public record CreateTransferRequest(
        @NotNull UUID sourceWalletId,
        @NotNull UUID destWalletId,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @NotNull @NotBlank @Size(min = 3, max = 3) String currency,
        @Size(max = 255) String description,
        @NotNull String reference) {
}
