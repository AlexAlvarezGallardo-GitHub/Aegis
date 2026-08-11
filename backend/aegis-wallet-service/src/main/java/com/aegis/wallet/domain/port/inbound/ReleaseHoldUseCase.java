package com.aegis.wallet.domain.port.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Port for releasing (cancelling) an ACTIVE funds hold.
 */
public interface ReleaseHoldUseCase {

    HoldResult release(ReleaseCommand command);

    record ReleaseCommand(UUID walletId, UUID holdId) {}

    record HoldResult(UUID holdId, UUID walletId, BigDecimal amount, String currency,
                      String reference, String status, BigDecimal availableBalance,
                      Instant createdAt, Instant expiresAt) {}
}
