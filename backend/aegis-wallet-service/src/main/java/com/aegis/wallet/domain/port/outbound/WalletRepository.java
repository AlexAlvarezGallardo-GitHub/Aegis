package com.aegis.wallet.domain.port.outbound;

import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(WalletId walletId);

    List<Wallet> findByUserId(UUID userId);

    long countByUserId(UUID userId);
}
