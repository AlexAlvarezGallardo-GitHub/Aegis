package com.aegis.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
