package com.aegis.payment.web.dto;

import com.aegis.payment.application.dto.RefundResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Web-layer response object for a refund.
 */
public record RefundResponse(
        UUID refundId,
        UUID paymentId,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String reason,
        String reference,
        String status,
        BigDecimal newBalance,
        Instant createdAt,
        Instant completedAt
) {

    /**
     * Maps an application {@link RefundResult} to a web response.
     *
     * @param result the application-layer result
     * @return the web response
     */
    public static RefundResponse from(RefundResult result) {
        return new RefundResponse(
                result.refundId(),
                result.paymentId(),
                result.walletId(),
                result.userId(),
                result.amount(),
                result.currency(),
                result.reason(),
                result.reference(),
                result.status().name(),
                result.newBalance(),
                result.createdAt(),
                result.completedAt()
        );
    }
}
