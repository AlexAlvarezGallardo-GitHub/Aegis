package com.aegis.payment.domain.port.outbound;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Outbound port for wallet operations (holds, settlement, release).
 */
public interface WalletGateway {

    /**
     * Creates a hold on the source wallet for the given amount.
     *
     * @param sourceWalletId the source wallet identifier
     * @param amount         the amount to hold
     * @param currency       the currency
     * @param reference      the idempotency reference
     * @return the hold identifier
     */
    UUID createHold(UUID sourceWalletId, BigDecimal amount, String currency, String reference);

    /**
     * Settles (consumes) a previously created hold, moving the funds from the
     * source wallet to the destination wallet in one transaction.
     *
     * @param transferId     the transfer identifier
     * @param holdId         the hold identifier
     * @param sourceWalletId the source wallet identifier
     * @param destWalletId   the destination wallet identifier
     * @param amount         the settled amount
     * @param currency       the currency
     * @return a record with the resulting balances
     */
    SettlementResult settle(UUID transferId, UUID holdId, UUID sourceWalletId, UUID destWalletId,
                            BigDecimal amount, String currency);

    /**
     * Releases a previously created hold without settling (saga compensation).
     *
     * @param walletId the wallet that holds the reservation
     * @param holdId   the hold identifier
     */
    void release(UUID walletId, UUID holdId);

    /**
     * Result of a settled transfer.
     *
     * @param sourceNewBalance the source wallet balance after settlement
     * @param destNewBalance   the destination wallet balance after settlement
     */
    record SettlementResult(BigDecimal sourceNewBalance, BigDecimal destNewBalance) {
    }
}
