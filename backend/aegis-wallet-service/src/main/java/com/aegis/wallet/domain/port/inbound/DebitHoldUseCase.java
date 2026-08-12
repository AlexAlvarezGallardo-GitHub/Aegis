package com.aegis.wallet.domain.port.inbound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Port for atomically debiting a hold for a payment: validates the hold, debits the
 * wallet (creating a PAYMENT ledger entry) inside a single transaction. Unlike
 * {@link SettleTransferUseCase}, this does NOT credit a destination wallet — it is
 * a debit-only operation for payments to external payees.
 */
public interface DebitHoldUseCase {

    /**
     * Debits the hold, creating a PAYMENT ledger entry.
     *
     * @param command the debit command
     * @return the debit result including the new wallet balance
     */
    DebitResult debit(DebitCommand command);

    record DebitCommand(UUID paymentId, UUID holdId, UUID walletId,
                        BigDecimal amount, String currency) {}

    record DebitResult(UUID paymentId, UUID holdId, UUID walletId,
                       BigDecimal newBalance, Instant timestamp) {}
}
