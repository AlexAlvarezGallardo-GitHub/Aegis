package com.aegis.payment.domain.port.inbound;

import com.aegis.payment.application.dto.RefundResult;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port for refunding a completed payment.
 */
public interface RefundPaymentUseCase {

    /**
     * Refunds a completed payment, crediting the payer's wallet.
     *
     * @param command the refund command
     * @return the refund result in its final state (COMPLETED or FAILED), including the wallet balance after the credit
     */
    RefundResult refund(RefundCommand command);

    /**
     * Inbound command for the refund-payment use case.
     */
    record RefundCommand(
            UUID paymentId,
            UUID userId,
            BigDecimal amount,
            String reason,
            String reference,
            boolean adminOverride
    ) {}
}
