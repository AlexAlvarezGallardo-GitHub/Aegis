package com.aegis.wallet.domain.port.outbound;

import com.aegis.wallet.domain.model.Hold;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for hold persistence operations.
 */
public interface HoldRepository {

    /**
     * Persists a new or updated hold.
     *
     * @param hold the hold to save
     * @return the saved hold (with any generated fields populated)
     */
    Hold save(Hold hold);

    /**
     * Finds a hold by its identifier.
     *
     * @param holdId the hold id
     * @return the hold if found
     */
    Optional<Hold> findById(UUID holdId);

    /**
     * Finds an ACTIVE hold by its reference (transfer id) for idempotency.
     *
     * @param reference the transfer id used as idempotency key
     * @return the ACTIVE hold if one exists
     */
    Optional<Hold> findActiveByReference(String reference);

    /**
     * Computes the sum of ACTIVE hold amounts for a given wallet.
     *
     * @param walletId the wallet whose active holds are summed
     * @return the total reserved amount (zero if no active holds)
     */
    BigDecimal sumActiveAmountByWalletId(UUID walletId);
}
