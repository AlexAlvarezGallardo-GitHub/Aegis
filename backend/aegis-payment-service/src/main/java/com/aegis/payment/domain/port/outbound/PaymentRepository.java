package com.aegis.payment.domain.port.outbound;

import com.aegis.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and retrieving payments.
 */
public interface PaymentRepository {

    /**
     * Persists a payment.
     *
     * @param payment the payment to save
     * @return the saved payment
     */
    Payment save(Payment payment);

    /**
     * Finds a payment by its identifier.
     *
     * @param paymentId the payment identifier
     * @return the payment, or empty if not found
     */
    Optional<Payment> findById(UUID paymentId);

    /**
     * Checks whether a payment with the given wallet and reference already exists.
     *
     * @param walletId  the wallet identifier
     * @param reference the idempotency reference
     * @return {@code true} if a payment already exists
     */
    boolean existsByWalletIdAndReference(UUID walletId, String reference);

    /**
     * Finds a payment by its identifier, acquiring a pessimistic write lock on the
     * underlying row (SELECT FOR UPDATE). Used during refund to prevent concurrent
     * refunds on the same payment.
     *
     * @param paymentId the payment identifier
     * @return the locked payment, or empty if not found
     */
    Optional<Payment> findByIdForUpdate(UUID paymentId);
}
