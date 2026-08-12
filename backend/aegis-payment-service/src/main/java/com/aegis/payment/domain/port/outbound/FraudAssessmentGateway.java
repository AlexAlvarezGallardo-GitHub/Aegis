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
     *
     * @param transactionId   the transaction identifier
     * @param transactionType the type of transaction (e.g. TRANSFER, PAYMENT)
     * @param sourceWalletId  the source wallet identifier
     * @param destWalletId    the destination wallet identifier (may be null for payments)
     * @param userId          the user identifier
     * @param amount          the transaction amount
     * @param currency        the currency code
     */
    record TransactionContext(
            UUID transactionId,
            String transactionType,
            UUID sourceWalletId,
            UUID destWalletId,
            UUID userId,
            BigDecimal amount,
            String currency
    ) {
        /**
         * Backwards-compatible constructor for transfers (transactionType = TRANSFER).
         */
        public TransactionContext(UUID transactionId, UUID sourceWalletId, UUID destWalletId,
                                  UUID userId, BigDecimal amount, String currency) {
            this(transactionId, "TRANSFER", sourceWalletId, destWalletId, userId, amount, currency);
        }
    }

    /**
     * Fraud assessment decision returned by the fraud service.
     */
    enum FraudDecision {
        APPROVE,
        REVIEW,
        REJECT
    }
}
