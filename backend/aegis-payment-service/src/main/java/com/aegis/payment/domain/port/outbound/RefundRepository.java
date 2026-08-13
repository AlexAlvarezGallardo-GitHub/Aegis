package com.aegis.payment.domain.port.outbound;

import com.aegis.payment.domain.model.Refund;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and retrieving refunds.
 */
public interface RefundRepository {

    /**
     * Persists a refund.
     *
     * @param refund the refund to save
     * @return the saved refund
     */
    Refund save(Refund refund);

    /**
     * Finds a refund by its identifier.
     *
     * @param refundId the refund identifier
     * @return the refund, or empty if not found
     */
    Optional<Refund> findById(UUID refundId);

    /**
     * Checks whether a refund with the given reference already exists.
     *
     * @param reference the idempotency reference
     * @return {@code true} if a refund already exists
     */
    boolean existsByReference(String reference);

    /**
     * Finds a refund by its reference (idempotency key).
     *
     * @param reference the idempotency reference
     * @return the refund, or empty if not found
     */
    Optional<Refund> findByReference(String reference);
}
