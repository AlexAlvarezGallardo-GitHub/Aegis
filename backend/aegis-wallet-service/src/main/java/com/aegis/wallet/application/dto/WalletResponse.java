package com.aegis.wallet.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID walletId,
        UUID userId,
        BigDecimal balance,
        String currency,
        String status,
        Instant createdAt
) {}
