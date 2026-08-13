package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;
import com.aegis.payment.domain.model.RefundStatus;

/**
 * Thrown when a refund state transition violates the state machine rules.
 */
public class InvalidRefundStateException extends AegisException {

    private static final String CODE = "INVALID_REFUND_STATE";

    /**
     * Creates a new invalid-state exception.
     *
     * @param current  the current refund status
     * @param intended the intended target status
     */
    public InvalidRefundStateException(RefundStatus current, RefundStatus intended) {
        super(CODE, ErrorStatus.BAD_REQUEST,
                "Invalid refund state transition from " + current + " to " + intended);
    }
}
