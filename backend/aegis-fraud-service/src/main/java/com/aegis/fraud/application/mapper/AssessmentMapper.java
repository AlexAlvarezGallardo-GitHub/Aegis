package com.aegis.fraud.application.mapper;

import com.aegis.fraud.application.dto.FraudAssessmentCommand;
import com.aegis.fraud.domain.port.inbound.AssessFraudUseCase;
import com.aegis.fraud.web.dto.AssessmentRequest;

/**
 * Maps web-layer DTOs to application-layer commands.
 */
public final class AssessmentMapper {

    private AssessmentMapper() {
    }

    /**
     * Converts a web {@link AssessmentRequest} into an application {@link FraudAssessmentCommand}.
     *
     * @param request the validated web request
     * @return the application-layer command
     */
    public static FraudAssessmentCommand toCommand(AssessmentRequest request) {
        return new FraudAssessmentCommand(
                request.transactionId(),
                request.transactionType(),
                request.amount(),
                request.currency(),
                request.sourceWalletId(),
                request.destWalletId(),
                request.userId(),
                request.countryCode()
        );
    }

    /**
     * Converts an application {@link FraudAssessmentCommand} into the inbound port
     * {@link AssessFraudUseCase.AssessmentCommand}.
     *
     * @param command the application-layer command
     * @return the inbound port command
     */
    public static AssessFraudUseCase.AssessmentCommand toUseCaseCommand(FraudAssessmentCommand command) {
        return new AssessFraudUseCase.AssessmentCommand(
                command.transactionId(),
                command.transactionType(),
                command.amount(),
                command.currency(),
                command.sourceWalletId(),
                command.destWalletId(),
                command.userId(),
                command.countryCode()
        );
    }
}
