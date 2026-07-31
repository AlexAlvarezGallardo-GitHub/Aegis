package com.aegis.bff.web.dto;

import java.math.BigDecimal;

/**
 * Request body for adjusting a wallet's balance.
 *
 * @param type   the adjustment type (e.g. CREDIT, DEBIT)
 * @param amount the amount to adjust
 * @param reason an optional human-readable reason for the adjustment
 */
public record AdjustBalanceRequest(String type, BigDecimal amount, String reason) {
}
