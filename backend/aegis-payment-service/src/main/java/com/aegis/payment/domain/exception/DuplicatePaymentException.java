package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

/**
 * Thrown when a payment with the same (walletId, reference) already exists.
 */
public class DuplicatePaymentException extends AegisException {

    private static final String CODE = "PAYMENT_DUPLICATE";

    /**
     * Creates a new duplicate-payment exception.
     *
     * @param reference the idempotency reference that collided
     */
    public DuplicatePaymentException(String reference) {
        super(CODE, ErrorStatus.CONFLICT, "Payment with reference already exists: " + reference);
    }
}
