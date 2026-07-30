package com.aegis.wallet.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletDetailResponse(
        UUID walletId,
        UUID userId,
        BigDecimal balance,
        String currency,
        String status,
        boolean premium,
        Instant createdAt,
        Instant updatedAt
) {}
