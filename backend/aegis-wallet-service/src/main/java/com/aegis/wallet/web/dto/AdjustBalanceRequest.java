package com.aegis.wallet.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Web-layer request body for adjusting a wallet balance.
 *
 * @param amount      the adjustment amount (positive for deposit, negative for withdrawal)
 * @param description an optional description for the ledger entry
 */
public record AdjustBalanceRequest(
        @NotNull(message = "Amount is required")
        BigDecimal amount,
        String description
) {
}
