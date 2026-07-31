package com.aegis.wallet.domain.port.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Inbound port for retrieving the detail of a single wallet.
 */
public interface GetWalletDetailUseCase {

    /**
     * Retrieves the detail of a wallet, validating ownership.
     *
     * @param walletId the wallet identifier
     * @param userId   the requesting user identifier
     * @return the wallet detail result
     * @throws com.aegis.wallet.domain.exception.WalletNotFoundException if the wallet does not exist
     *         or does not belong to the user
     */
    Result getDetail(UUID walletId, UUID userId);

    /**
     * Detail result for a wallet.
     *
     * @param walletId  the wallet identifier
     * @param userId    the owner user identifier
     * @param balance   the current balance
     * @param currency  the wallet currency
     * @param status    the wallet status
     * @param premium   whether the wallet qualifies as premium
     * @param createdAt the creation timestamp
     * @param updatedAt the last update timestamp
     */
    record Result(UUID walletId, UUID userId, BigDecimal balance, String currency,
                  String status, boolean premium, Instant createdAt, Instant updatedAt) {
    }
}
