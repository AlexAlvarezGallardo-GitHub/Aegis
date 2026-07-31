package com.aegis.fraud.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Application-layer command for fraud assessment. Free of web validation annotations.
 */
public record FraudAssessmentCommand(
        UUID transactionId,
        String transactionType,
        BigDecimal amount,
        String currency,
        UUID sourceWalletId,
        UUID destWalletId,
        UUID userId,
        String countryCode
) {}
