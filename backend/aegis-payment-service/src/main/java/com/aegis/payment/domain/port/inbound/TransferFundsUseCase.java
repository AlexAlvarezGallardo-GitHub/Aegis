package com.aegis.payment.domain.port.inbound;

import com.aegis.payment.domain.model.Transfer;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port for initiating a funds transfer between two wallets.
 */
public interface TransferFundsUseCase {

    /**
     * Executes the transfer-funds use case.
     *
     * @param command the transfer command
     * @return the created transfer in its initial state
     */
    Transfer execute(TransferCommand command);

    /**
     * Inbound command for the transfer-funds use case.
     */
    record TransferCommand(
            UUID sourceWalletId,
            UUID destWalletId,
            UUID userId,
            BigDecimal amount,
            String currency,
            String description,
            String reference
    ) {}
}
