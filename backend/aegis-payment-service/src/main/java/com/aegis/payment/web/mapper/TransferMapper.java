package com.aegis.payment.web.mapper;

import com.aegis.payment.domain.port.inbound.TransferFundsUseCase;
import com.aegis.payment.web.dto.TransferRequest;

/**
 * Maps web-layer DTOs to application commands and domain use-case commands.
 */
public final class TransferMapper {

    private TransferMapper() {
    }

    /**
     * Converts a web {@link TransferRequest} into the inbound port
     * {@link TransferFundsUseCase.TransferCommand}.
     *
     * @param request the validated web request
     * @return the inbound port command
     */
    public static TransferFundsUseCase.TransferCommand toCommand(TransferRequest request) {
        return new TransferFundsUseCase.TransferCommand(
                request.sourceWalletId(),
                request.destWalletId(),
                request.userId(),
                request.amount(),
                request.currency(),
                request.description(),
                request.reference()
        );
    }
}
