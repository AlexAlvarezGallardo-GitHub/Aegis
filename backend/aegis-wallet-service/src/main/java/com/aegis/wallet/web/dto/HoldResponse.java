package com.aegis.wallet.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Web-layer response body for a funds hold.
 */
public record HoldResponse(
        UUID holdId,
        UUID walletId,
        BigDecimal amount,
        String currency,
        String reference,
        String status,
        BigDecimal availableBalance,
        Instant createdAt,
        Instant expiresAt
) {
}
