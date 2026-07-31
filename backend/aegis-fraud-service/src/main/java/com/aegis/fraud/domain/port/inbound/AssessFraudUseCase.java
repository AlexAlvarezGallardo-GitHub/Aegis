package com.aegis.fraud.domain.port.inbound;

import com.aegis.fraud.domain.model.FraudAssessment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port for assessing a transaction against the fraud rules engine.
 */
public interface AssessFraudUseCase {

    FraudAssessment assess(AssessmentCommand command);

    FraudAssessment findById(UUID assessmentId);

    record AssessmentCommand(
            UUID transactionId,
            String transactionType,
            BigDecimal amount,
            String currency,
            UUID sourceWalletId,
            UUID destWalletId,
            UUID userId,
            String countryCode
    ) {}
}
