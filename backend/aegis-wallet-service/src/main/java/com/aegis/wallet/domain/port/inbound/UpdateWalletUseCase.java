package com.aegis.wallet.domain.port.inbound;

import com.aegis.wallet.domain.model.WalletStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Use case for managing wallets — adjusting balance, changing status, and retrieving details.
 */
public interface UpdateWalletUseCase {

    WalletDetailResult adjustBalance(AdjustBalanceCommand command);

    WalletDetailResult changeStatus(StatusChangeCommand command);

    record AdjustBalanceCommand(UUID walletId, UUID userId, BigDecimal amount,
                                String description, String correlationId) {}

    record StatusChangeCommand(UUID walletId, UUID userId, WalletStatus newStatus) {}

    record WalletDetailResult(UUID walletId, UUID userId, BigDecimal balance, String currency,
                              String status, boolean premium, Instant createdAt, Instant updatedAt) {}
}
