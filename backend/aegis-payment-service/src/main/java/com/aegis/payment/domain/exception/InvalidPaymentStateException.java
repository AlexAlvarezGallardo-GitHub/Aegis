package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;
import com.aegis.payment.domain.model.PaymentStatus;

/**
 * Thrown when a payment state transition violates the state machine rules.
 */
public class InvalidPaymentStateException extends AegisException {

    private static final String CODE = "INVALID_PAYMENT_STATE";

    /**
     * Creates a new invalid-state exception.
     *
     * @param current   the current payment status
     * @param intended  the intended target status
     */
    public InvalidPaymentStateException(PaymentStatus current, PaymentStatus intended) {
        super(CODE, ErrorStatus.BAD_REQUEST,
                "Invalid payment state transition from " + current + " to " + intended);
    }
}
