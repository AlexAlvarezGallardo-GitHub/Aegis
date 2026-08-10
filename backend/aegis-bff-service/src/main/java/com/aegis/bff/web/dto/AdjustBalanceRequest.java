package com.aegis.bff.web.dto;

import java.math.BigDecimal;

/**
 * Request body for adjusting a wallet's balance.
 *
 * @param amount      the adjustment amount (positive for credit, negative for debit)
 * @param description an optional human-readable description for the adjustment
 */
public record AdjustBalanceRequest(BigDecimal amount, String description) {
}
