package com.aegis.payment.web.mapper;

import com.aegis.payment.domain.model.Payee;
import com.aegis.payment.domain.model.PayeeType;
import com.aegis.payment.domain.port.inbound.ExecutePaymentUseCase;
import com.aegis.payment.web.dto.PaymentRequest;

/**
 * Maps web-layer DTOs to application commands and domain use-case commands.
 */
public final class PaymentMapper {

    private PaymentMapper() {
    }

    /**
     * Converts a web {@link PaymentRequest} into the inbound port
     * {@link ExecutePaymentUseCase.PaymentCommand}.
     *
     * @param request the validated web request
     * @return the inbound port command
     */
    public static ExecutePaymentUseCase.PaymentCommand toCommand(PaymentRequest request) {
        Payee payee = new Payee(
                request.payee().name(),
                request.payee().id(),
                PayeeType.valueOf(request.payee().type().toUpperCase())
        );
        return new ExecutePaymentUseCase.PaymentCommand(
                request.walletId(),
                request.userId(),
                request.amount(),
                request.currency(),
                payee,
                request.description(),
                request.reference()
        );
    }
}
