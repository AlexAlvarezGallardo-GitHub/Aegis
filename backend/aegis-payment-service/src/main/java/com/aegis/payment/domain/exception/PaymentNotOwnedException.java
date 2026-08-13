package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when a user attempts to refund a payment they do not own (and is not an admin).
 */
public class PaymentNotOwnedException extends AegisException {

    private static final String CODE = "PAYMENT_NOT_OWNED";

    /**
     * Creates a new exception for the given payment and user identifiers.
     *
     * @param paymentId the payment identifier
     * @param userId    the user attempting the refund
     */
    public PaymentNotOwnedException(UUID paymentId, UUID userId) {
        super(CODE, ErrorStatus.FORBIDDEN,
                "Payment " + paymentId + " is not owned by user " + userId);
    }
}
