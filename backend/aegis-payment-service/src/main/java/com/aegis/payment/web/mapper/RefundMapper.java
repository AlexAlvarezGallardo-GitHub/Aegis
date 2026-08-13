package com.aegis.payment.web.mapper;

import com.aegis.payment.domain.port.inbound.RefundPaymentUseCase;
import com.aegis.payment.web.dto.RefundRequest;

import java.util.UUID;

/**
 * Maps web-layer DTOs to application commands for the refund use case.
 */
public final class RefundMapper {

    private RefundMapper() {
    }

    /**
     * Converts a web {@link RefundRequest} into the inbound port
     * {@link RefundPaymentUseCase.RefundCommand}.
     *
     * @param paymentId     the payment to refund
     * @param request       the validated web request
     * @param userId        the authenticated user id (from the X-User-Id header)
     * @param adminOverride whether the user has admin override
     * @return the inbound port command
     */
    public static RefundPaymentUseCase.RefundCommand toCommand(UUID paymentId, RefundRequest request,
                                                                UUID userId, boolean adminOverride) {
        return new RefundPaymentUseCase.RefundCommand(
                paymentId,
                userId,
                request.amount(),
                request.reason(),
                request.reference(),
                adminOverride
        );
    }
}
