package com.aegis.wallet.domain.port.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Port for atomically settling a transfer: validates the hold, debits the source
 * wallet and credits the destination wallet inside a single transaction.
 */
public interface SettleTransferUseCase {

    SettleResult settle(SettleCommand command);

    record SettleCommand(UUID transferId, UUID holdId, UUID sourceWalletId,
                         UUID destWalletId, BigDecimal amount, String currency) {}

    record SettleResult(UUID transferId, UUID holdId, UUID sourceWalletId,
                        BigDecimal sourceNewBalance, UUID destWalletId,
                        BigDecimal destNewBalance, Instant timestamp) {}
}
