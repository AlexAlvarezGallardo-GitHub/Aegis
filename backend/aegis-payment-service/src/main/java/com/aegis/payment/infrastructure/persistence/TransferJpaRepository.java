package com.aegis.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data repository for transfer entities.
 */
public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, UUID> {

    /**
     * Checks whether a transfer with the given source wallet and reference already exists.
     *
     * @param sourceWalletId the source wallet identifier
     * @param reference      the idempotency reference
     * @return {@code true} if a transfer already exists
     */
    boolean existsBySourceWalletIdAndReference(UUID sourceWalletId, String reference);
}
