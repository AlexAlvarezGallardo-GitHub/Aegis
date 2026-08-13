package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when a payment has already been refunded.
 */
public class PaymentAlreadyRefundedException extends AegisException {

    private static final String CODE = "PAYMENT_ALREADY_REFUNDED";

    /**
     * Creates a new exception for the given payment identifier.
     *
     * @param paymentId the payment identifier that has already been refunded
     */
    public PaymentAlreadyRefundedException(UUID paymentId) {
        super(CODE, ErrorStatus.CONFLICT, "Payment already refunded: " + paymentId);
    }
}
