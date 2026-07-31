package com.aegis.wallet.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DepositReceipt(
        UUID depositId,
        UUID walletId,
        BigDecimal newBalance,
        BigDecimal amount,
        String currency,
        String source,
        String reference,
        Instant timestamp
) {}
