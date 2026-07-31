package com.aegis.wallet.domain.port.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Inbound port for listing wallets belonging to a user.
 */
public interface ListWalletsUseCase {

    /**
     * Lists all wallets for the given user.
     *
     * @param userId the user identifier
     * @return the list of wallet summaries
     */
    List<Result> listByUser(UUID userId);

    /**
     * Summary result for a wallet listing.
     *
     * @param walletId  the wallet identifier
     * @param userId    the owner user identifier
     * @param balance   the current balance
     * @param currency  the wallet currency
     * @param status    the wallet status
     * @param premium   whether the wallet qualifies as premium
     * @param createdAt the creation timestamp
     */
    record Result(UUID walletId, UUID userId, BigDecimal balance, String currency,
                  String status, boolean premium, Instant createdAt) {
    }
}
