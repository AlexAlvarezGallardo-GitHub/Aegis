package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when a refund amount exceeds the original payment amount.
 */
public class RefundExceedsPaymentException extends AegisException {

    private static final String CODE = "REFUND_EXCEEDS_PAYMENT";

    /**
     * Creates a new exception for the given payment identifier.
     *
     * @param paymentId the payment identifier
     */
    public RefundExceedsPaymentException(UUID paymentId) {
        super(CODE, ErrorStatus.UNPROCESSABLE_ENTITY,
                "Refund amount exceeds payment amount: " + paymentId);
    }
}
