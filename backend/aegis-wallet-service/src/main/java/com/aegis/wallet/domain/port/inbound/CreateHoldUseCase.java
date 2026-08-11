package com.aegis.wallet.domain.port.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Port for reserving (holding) funds on a wallet pending a transfer.
 */
public interface CreateHoldUseCase {

    HoldResult createHold(CreateHoldCommand command);

    record CreateHoldCommand(UUID walletId, BigDecimal amount, String currency, String reference) {}

    record HoldResult(UUID holdId, UUID walletId, BigDecimal amount, String currency,
                      String reference, String status, BigDecimal availableBalance,
                      Instant createdAt, Instant expiresAt) {}
}
