package com.aegis.payment.domain.port.inbound;

import com.aegis.payment.domain.model.Payment;

import java.util.UUID;

/**
 * Port for retrieving a payment by its identifier.
 */
public interface GetPaymentUseCase {

    /**
     * Finds a payment by its identifier.
     *
     * @param paymentId the payment identifier
     * @return the payment
     * @throws com.aegis.payment.domain.exception.PaymentNotFoundException if not found
     */
    Payment findById(UUID paymentId);
}
