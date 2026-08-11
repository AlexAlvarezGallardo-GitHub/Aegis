package com.aegis.payment.web.dto;

import com.aegis.payment.application.dto.TransferResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Web-layer response object for a transfer.
 */
public record TransferResponse(
        UUID transferId,
        UUID sourceWalletId,
        UUID destWalletId,
        UUID userId,
        BigDecimal amount,
        String currency,
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
     * Maps an application {@link TransferResult} to a web response.
     *
     * @param result the application-layer result
     * @return the web response
     */
    public static TransferResponse from(TransferResult result) {
        return new TransferResponse(
                result.transferId(),
                result.sourceWalletId(),
                result.destWalletId(),
                result.userId(),
                result.amount(),
                result.currency(),
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
