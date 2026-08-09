package com.aegis.wallet.domain.port.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Port for reversing a previously applied deposit.
 */
public interface ReverseDepositUseCase {

    ReverseResult reverse(ReverseCommand command);

    record ReverseCommand(UUID walletId, UUID userId, UUID depositEntryId,
                          String reference, String correlationId) {}

    record ReverseResult(UUID reversalId, UUID walletId, BigDecimal newBalance,
                         BigDecimal reversedAmount, String currency, Instant timestamp) {}
}
