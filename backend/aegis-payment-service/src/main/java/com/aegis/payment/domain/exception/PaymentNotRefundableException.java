package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when a payment is not in a refundable state (e.g. FAILED, PENDING, already REFUNDED).
 */
public class PaymentNotRefundableException extends AegisException {

    private static final String CODE = "PAYMENT_NOT_REFUNDABLE";

    /**
     * Creates a new exception for the given payment identifier.
     *
     * @param paymentId the payment identifier that is not refundable
     */
    public PaymentNotRefundableException(UUID paymentId) {
        super(CODE, ErrorStatus.UNPROCESSABLE_ENTITY, "Payment not refundable: " + paymentId);
    }
}
