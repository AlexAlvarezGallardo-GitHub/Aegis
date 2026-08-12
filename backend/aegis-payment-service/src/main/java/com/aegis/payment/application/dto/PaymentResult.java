package com.aegis.payment.application.dto;

import com.aegis.payment.domain.model.Payee;
import com.aegis.payment.domain.model.Payment;
import com.aegis.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Application-layer response representing a payment.
 */
public record PaymentResult(
        UUID paymentId,
        UUID walletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        Payee payee,
        String description,
        String reference,
        PaymentStatus status,
        UUID fraudAssessmentId,
        UUID holdId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {

    /**
     * Maps a domain {@link Payment} to a {@link PaymentResult}.
     *
     * @param payment the domain payment
     * @return the application-layer result
     */
    public static PaymentResult from(Payment payment) {
        return new PaymentResult(
                payment.getId(),
                payment.getWalletId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPayee(),
                payment.getDescription(),
                payment.getReference(),
                payment.getStatus(),
                payment.getFraudAssessmentId(),
                payment.getHoldId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getCompletedAt()
        );
    }
}
