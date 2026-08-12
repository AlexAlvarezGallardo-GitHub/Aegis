package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when the fraud service rejects a payment.
 */
public class PaymentRejectedException extends AegisException {

    private static final String CODE = "PAYMENT_REJECTED_BY_FRAUD";

    /**
     * Creates a new payment-rejected exception.
     *
     * @param paymentId the payment that was rejected
     */
    public PaymentRejectedException(UUID paymentId) {
        super(CODE, ErrorStatus.UNPROCESSABLE_ENTITY,
                "Payment rejected by fraud assessment: " + paymentId);
    }
}
