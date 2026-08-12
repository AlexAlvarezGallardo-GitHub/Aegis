package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when the wallet service rejects or fails a hold/settle/release step of
 * the payment saga. Triggers saga compensation (hold release).
 */
public class PaymentSettlementFailedException extends AegisException {

    private static final String CODE = "SETTLEMENT_FAILED";

    /**
     * Creates a new settlement-failed exception.
     *
     * @param message the error detail
     */
    public PaymentSettlementFailedException(String message) {
        super(CODE, ErrorStatus.UNPROCESSABLE_ENTITY, message);
    }

    /**
     * Creates a new settlement-failed exception with a custom error code
     * (e.g. the wallet service's INSUFFICIENT_FUNDS propagated through the saga).
     *
     * @param code    the error code to surface
     * @param message the error detail
     * @param cause   the underlying cause
     */
    public PaymentSettlementFailedException(String code, String message, Throwable cause) {
        super(code, ErrorStatus.UNPROCESSABLE_ENTITY, message, cause);
    }

    /**
     * Creates a new settlement-failed exception with a cause.
     *
     * @param message the error detail
     * @param cause   the underlying cause
     */
    public PaymentSettlementFailedException(String message, Throwable cause) {
        super(CODE, ErrorStatus.UNPROCESSABLE_ENTITY, message, cause);
    }

    /**
     * Creates a new settlement-failed exception for a specific payment.
     *
     * @param paymentId the payment whose settlement failed
     * @param message   the error detail
     */
    public PaymentSettlementFailedException(UUID paymentId, String message) {
        super(CODE, ErrorStatus.UNPROCESSABLE_ENTITY,
                "Settlement failed for payment " + paymentId + ": " + message);
    }
}
