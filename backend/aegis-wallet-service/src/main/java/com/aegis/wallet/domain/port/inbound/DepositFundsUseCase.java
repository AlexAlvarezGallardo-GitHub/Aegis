package com.aegis.wallet.domain.port.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Port for depositing funds into a wallet.
 */
public interface DepositFundsUseCase {

    DepositResult deposit(DepositCommand command);

    record DepositCommand(UUID walletId, UUID userId, BigDecimal amount, String currency,
                          String source, String reference, String correlationId) {}

    record DepositResult(UUID depositId, UUID walletId, BigDecimal newBalance, BigDecimal amount,
                         String currency, String source, String reference, Instant timestamp) {}
}
