package com.aegis.wallet.domain.port.outbound;

import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for wallet persistence operations.
 */
public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(WalletId walletId);

    List<Wallet> findByUserId(UUID userId);

    long countByUserId(UUID userId);

    /**
     * Finds a wallet by its identifier, acquiring a pessimistic write lock on the
     * underlying row (SELECT FOR UPDATE). The wallet is loaded together with its
     * ledger entries.
     *
     * @param walletId the wallet id
     * @return the locked wallet if found
     */
    Optional<Wallet> findByIdForUpdate(WalletId walletId);
}
