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
     * Settles (consumes) a previously created hold.
     *
     * @param holdId the hold identifier
     */
    void settle(UUID holdId);

    /**
     * Releases a previously created hold without settling.
     *
     * @param holdId the hold identifier
     */
    void release(UUID holdId);
}
