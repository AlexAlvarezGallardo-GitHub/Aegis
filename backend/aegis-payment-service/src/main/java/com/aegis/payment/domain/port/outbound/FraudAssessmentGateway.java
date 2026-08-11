package com.aegis.payment.domain.port.outbound;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Outbound port for synchronous communication with the fraud service.
 */
public interface FraudAssessmentGateway {

    /**
     * Submits a transaction context to the fraud service for assessment.
     *
     * @param context the transaction context
     * @return the fraud assessment decision
     */
    FraudDecision assess(TransactionContext context);

    /**
     * Transaction context sent to the fraud service.
     */
    record TransactionContext(
            UUID transactionId,
            UUID sourceWalletId,
            UUID destWalletId,
            UUID userId,
            BigDecimal amount,
            String currency
    ) {}

    /**
     * Fraud assessment decision returned by the fraud service.
     */
    enum FraudDecision {
        APPROVE,
        REVIEW,
        REJECT
    }
}
