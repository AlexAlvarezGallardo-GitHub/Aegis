package com.aegis.payment.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for payment entities.
 */
public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    /**
     * Checks whether a payment with the given wallet and reference already exists.
     *
     * @param walletId  the wallet identifier
     * @param reference the idempotency reference
     * @return {@code true} if a payment already exists
     */
    boolean existsByWalletIdAndReference(UUID walletId, String reference);

    /**
     * Finds a payment by its identifier, acquiring a pessimistic write lock (SELECT FOR UPDATE).
     *
     * @param id the payment identifier
     * @return the locked payment, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentJpaEntity> findByIdForUpdate(UUID id);
}
