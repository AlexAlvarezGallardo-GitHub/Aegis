package com.aegis.payment.domain.port.inbound;

import com.aegis.payment.domain.model.Payee;
import com.aegis.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port for executing a payment from a wallet to a payee.
 */
public interface ExecutePaymentUseCase {

    /**
     * Executes the payment use case.
     *
     * @param command the payment command
     * @return the payment in its final state (COMPLETED or FAILED)
     */
    Payment execute(PaymentCommand command);

    /**
     * Inbound command for the execute-payment use case.
     */
    record PaymentCommand(
            UUID walletId,
            UUID userId,
            BigDecimal amount,
            String currency,
            Payee payee,
            String description,
            String reference
    ) {}
}
