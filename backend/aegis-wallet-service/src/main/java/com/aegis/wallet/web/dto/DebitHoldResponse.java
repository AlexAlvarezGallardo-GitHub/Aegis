package com.aegis.wallet.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Web-layer response for a hold debit operation.
 */
public record DebitHoldResponse(
        UUID paymentId,
        UUID holdId,
        UUID walletId,
        BigDecimal newBalance,
        Instant timestamp
) {}
