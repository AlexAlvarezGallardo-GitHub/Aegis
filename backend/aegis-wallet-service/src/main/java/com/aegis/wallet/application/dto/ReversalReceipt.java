package com.aegis.wallet.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Application-layer response returned after a deposit reversal.
 *
 * @param reversalId     id of the new REVERSAL ledger entry
 * @param walletId       id of the affected wallet
 * @param newBalance     wallet balance after the reversal
 * @param reversedAmount amount reversed
 * @param currency       ISO-4217 currency code
 * @param timestamp      when the reversal was applied
 */
public record ReversalReceipt(
        UUID reversalId,
        UUID walletId,
        BigDecimal newBalance,
        BigDecimal reversedAmount,
        String currency,
        Instant timestamp
) {
}
