package com.aegis.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for hold entities.
 */
public interface HoldJpaRepository extends JpaRepository<HoldJpaEntity, UUID> {

    /**
     * Finds an ACTIVE hold by its idempotency reference.
     */
    @Query("SELECT h FROM HoldJpaEntity h WHERE h.reference = :reference AND h.status = 'ACTIVE'")
    Optional<HoldJpaEntity> findActiveByReference(@Param("reference") String reference);

    /**
     * Sum of amounts of all ACTIVE holds for a wallet. Returns zero if none.
     */
    @Query("SELECT COALESCE(SUM(h.amount), 0) FROM HoldJpaEntity h "
            + "WHERE h.walletId = :walletId AND h.status = 'ACTIVE'")
    BigDecimal sumActiveAmountByWalletId(@Param("walletId") UUID walletId);
}
