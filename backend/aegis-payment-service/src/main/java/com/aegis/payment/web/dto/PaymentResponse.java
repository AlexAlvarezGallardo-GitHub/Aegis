package com.aegis.payment.web.dto;

import com.aegis.payment.application.dto.PaymentResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Web-layer response object for a payment.
 */
public record PaymentResponse(
        UUID paymentId,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        PayeeResponse payee,
        String description,
        String reference,
        String status,
        UUID fraudAssessmentId,
        UUID holdId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {

    /**
     * Maps an application {@link PaymentResult} to a web response.
     *
     * @param result the application-layer result
     * @return the web response
     */
    public static PaymentResponse from(PaymentResult result) {
        return new PaymentResponse(
                result.paymentId(),
                result.walletId(),
                result.userId(),
                result.amount(),
                result.currency(),
                new PayeeResponse(
                        result.payee().name(),
                        result.payee().id(),
                        result.payee().type().name()
                ),
                result.description(),
                result.reference(),
                result.status().name(),
                result.fraudAssessmentId(),
                result.holdId(),
                result.failureReason(),
                result.createdAt(),
                result.updatedAt(),
                result.completedAt()
        );
    }
}
