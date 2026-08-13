package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

/**
 * Thrown when a refund with the same reference already exists.
 */
public class RefundAlreadyExistsException extends AegisException {

    private static final String CODE = "REFUND_ALREADY_EXISTS";

    /**
     * Creates a new duplicate-refund exception.
     *
     * @param reference the idempotency reference that collided
     */
    public RefundAlreadyExistsException(String reference) {
        super(CODE, ErrorStatus.CONFLICT, "Refund with reference already exists: " + reference);
    }
}
