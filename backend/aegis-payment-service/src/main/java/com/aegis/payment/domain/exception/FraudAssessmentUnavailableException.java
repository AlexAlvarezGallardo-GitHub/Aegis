package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

/**
 * Thrown when the fraud service is unavailable or times out.
 */
public class FraudAssessmentUnavailableException extends AegisException {

    private static final String CODE = "FRAUD_UNAVAILABLE";

    /**
     * Creates a new fraud-unavailable exception.
     *
     * @param cause the underlying cause
     */
    public FraudAssessmentUnavailableException(Throwable cause) {
        super(CODE, ErrorStatus.SERVICE_UNAVAILABLE,
                "Fraud assessment service is unavailable", cause);
    }

    /**
     * Creates a new fraud-unavailable exception with a custom message.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public FraudAssessmentUnavailableException(String message, Throwable cause) {
        super(CODE, ErrorStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
