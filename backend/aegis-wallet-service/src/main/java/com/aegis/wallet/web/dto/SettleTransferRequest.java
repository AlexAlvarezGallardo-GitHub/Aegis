package com.aegis.wallet.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Web-layer request body for settling a transfer.
 */
public record SettleTransferRequest(
        @NotNull(message = "Transfer id is required")
        UUID transferId,

        @NotNull(message = "Hold id is required")
        UUID holdId,

        @NotNull(message = "Source wallet id is required")
        UUID sourceWalletId,

        @NotNull(message = "Destination wallet id is required")
        UUID destWalletId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency
) {
}
