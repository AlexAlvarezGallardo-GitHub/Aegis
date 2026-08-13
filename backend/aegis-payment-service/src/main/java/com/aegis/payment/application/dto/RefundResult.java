package com.aegis.payment.application.dto;

import com.aegis.payment.domain.model.Refund;
import com.aegis.payment.domain.model.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Application-layer response representing a refund.
 */
public record RefundResult(
        UUID refundId,
        UUID paymentId,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String reason,
        String reference,
        RefundStatus status,
        BigDecimal newBalance,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {

    /**
     * Maps a domain {@link Refund} to a {@link RefundResult}.
     *
     * @param refund the domain refund
     * @return the application-layer result
     */
    public static RefundResult from(Refund refund) {
        return new RefundResult(
                refund.getId(),
                refund.getPaymentId(),
                refund.getWalletId(),
                refund.getUserId(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getReason(),
                refund.getReference(),
                refund.getStatus(),
                null,
                refund.getCreatedAt(),
                refund.getUpdatedAt(),
                refund.getCompletedAt()
        );
    }

    /**
     * Maps a domain {@link Refund} with a wallet balance to a {@link RefundResult}.
     *
     * @param refund     the domain refund
     * @param newBalance the wallet balance after the refund credit
     * @return the application-layer result
     */
    public static RefundResult from(Refund refund, BigDecimal newBalance) {
        return new RefundResult(
                refund.getId(),
                refund.getPaymentId(),
                refund.getWalletId(),
                refund.getUserId(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getReason(),
                refund.getReference(),
                refund.getStatus(),
                newBalance,
                refund.getCreatedAt(),
                refund.getUpdatedAt(),
                refund.getCompletedAt()
        );
    }
}
