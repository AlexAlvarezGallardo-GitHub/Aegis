package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

/**
 * Thrown when the fraud service is unavailable or times out during payment assessment.
 */
public class PaymentAssessmentUnavailableException extends AegisException {

    private static final String CODE = "FRAUD_UNAVAILABLE";

    /**
     * Creates a new fraud-unavailable exception with a custom message.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public PaymentAssessmentUnavailableException(String message, Throwable cause) {
        super(CODE, ErrorStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
