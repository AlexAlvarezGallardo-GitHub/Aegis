package com.aegis.fraud.application.service.rule;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionContext(
        UUID transactionId,
        String transactionType,
        BigDecimal amount,
        String currency,
        UUID sourceWalletId,
        UUID destWalletId,
        UUID userId,
        String countryCode,
        String expectedCountryCode,
        int recentTransactionsCount,
        Instant timestamp
) {}
