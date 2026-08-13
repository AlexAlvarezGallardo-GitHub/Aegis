package com.aegis.wallet.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Web-layer response object for a refund credit.
 */
public record CreditRefundResponse(
        UUID refundId,
        UUID walletId,
        BigDecimal newBalance,
        Instant timestamp
) {}
