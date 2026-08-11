package com.aegis.payment.application.dto;

import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.domain.model.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Application-layer response representing a transfer.
 */
public record TransferResult(
        UUID transferId,
        UUID sourceWalletId,
        UUID destWalletId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String description,
        String reference,
        TransferStatus status,
        UUID fraudAssessmentId,
        UUID holdId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {

    /**
     * Maps a domain {@link Transfer} to a {@link TransferResult}.
     *
     * @param transfer the domain transfer
     * @return the application-layer result
     */
    public static TransferResult from(Transfer transfer) {
        return new TransferResult(
                transfer.getId(),
                transfer.getSourceWalletId(),
                transfer.getDestWalletId(),
                transfer.getUserId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getDescription(),
                transfer.getReference(),
                transfer.getStatus(),
                transfer.getFraudAssessmentId(),
                transfer.getHoldId(),
                transfer.getFailureReason(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt(),
                transfer.getCompletedAt()
        );
    }
}
