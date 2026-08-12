package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when a payment cannot be found by its identifier.
 */
public class PaymentNotFoundException extends AegisException {

    private static final String CODE = "PAYMENT_NOT_FOUND";

    /**
     * Creates a new exception for the given payment identifier.
     *
     * @param paymentId the payment identifier that could not be found
     */
    public PaymentNotFoundException(UUID paymentId) {
        super(CODE, ErrorStatus.NOT_FOUND, "Payment not found: " + paymentId);
    }
}
