package com.aegis.payment.domain.port.inbound;

import com.aegis.payment.domain.model.Refund;

import java.util.UUID;

/**
 * Port for retrieving refund details.
 */
public interface GetRefundUseCase {

    /**
     * Finds a refund by its identifier.
     *
     * @param refundId the refund identifier
     * @return the refund
     */
    Refund findById(UUID refundId);
}
