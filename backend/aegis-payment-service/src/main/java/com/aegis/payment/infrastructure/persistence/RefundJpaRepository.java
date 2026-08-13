package com.aegis.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for refund entities.
 */
public interface RefundJpaRepository extends JpaRepository<RefundJpaEntity, UUID> {

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
    Optional<RefundJpaEntity> findByReference(String reference);
}
