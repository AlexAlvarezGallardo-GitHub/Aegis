package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when a refund cannot be found by its identifier.
 */
public class RefundNotFoundException extends AegisException {

    private static final String CODE = "REFUND_NOT_FOUND";

    /**
     * Creates a new exception for the given refund identifier.
     *
     * @param refundId the refund identifier that could not be found
     */
    public RefundNotFoundException(UUID refundId) {
        super(CODE, ErrorStatus.NOT_FOUND, "Refund not found: " + refundId);
    }
}
