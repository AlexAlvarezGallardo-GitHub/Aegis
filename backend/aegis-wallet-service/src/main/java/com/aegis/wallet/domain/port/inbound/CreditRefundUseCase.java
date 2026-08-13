package com.aegis.wallet.domain.port.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Port for crediting a wallet with a refund. Creates a REFUND ledger entry
 * and increases the wallet balance in one transaction. Idempotent by reference.
 */
public interface CreditRefundUseCase {

    /**
     * Credits the wallet with a refund.
     *
     * @param command the credit-refund command
     * @return the credit result including the new wallet balance
     */
    CreditResult credit(CreditCommand command);

    record CreditCommand(UUID refundId, UUID walletId, BigDecimal amount, String currency) {}

    record CreditResult(UUID refundId, UUID walletId, BigDecimal newBalance, Instant timestamp) {}
}
