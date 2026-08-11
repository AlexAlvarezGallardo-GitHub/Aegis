package com.aegis.wallet.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Web-layer response body for a settled transfer.
 */
public record SettleTransferResponse(
        UUID transferId,
        UUID holdId,
        UUID sourceWalletId,
        BigDecimal sourceNewBalance,
        UUID destWalletId,
        BigDecimal destNewBalance,
        Instant timestamp
) {
}
