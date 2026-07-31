package com.aegis.fraud.application.mapper;

import com.aegis.fraud.application.dto.FraudAssessmentCommand;
import com.aegis.fraud.domain.port.inbound.AssessFraudUseCase;
import com.aegis.fraud.web.dto.AssessmentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssessmentMapper - DTO Mapping")
class AssessmentMapperTest {

    @Nested
    @DisplayName("When mapping AssessmentRequest to FraudAssessmentCommand")
    class WhenMappingRequestToCommand {

        @Test
        @DisplayName("Should map all fields correctly")
        void shouldMapAllFieldsCorrectly() {
            UUID transactionId = UUID.randomUUID();
            UUID sourceWalletId = UUID.randomUUID();
            UUID destWalletId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            AssessmentRequest request = new AssessmentRequest(
                    transactionId, "TRANSFER", new BigDecimal("1500.50"), "EUR",
                    sourceWalletId, destWalletId, userId, "ES");

            FraudAssessmentCommand command = AssessmentMapper.toCommand(request);

            assertEquals(transactionId, command.transactionId());
            assertEquals("TRANSFER", command.transactionType());
            assertEquals(new BigDecimal("1500.50"), command.amount());
            assertEquals("EUR", command.currency());
            assertEquals(sourceWalletId, command.sourceWalletId());
            assertEquals(destWalletId, command.destWalletId());
            assertEquals(userId, command.userId());
            assertEquals("ES", command.countryCode());
        }

        @Test
        @DisplayName("Should handle null optional fields")
        void shouldHandleNullOptionalFields() {
            AssessmentRequest request = new AssessmentRequest(
                    UUID.randomUUID(), "TRANSFER", BigDecimal.TEN, "EUR",
                    null, null, UUID.randomUUID(), null);

            FraudAssessmentCommand command = AssessmentMapper.toCommand(request);

            assertNull(command.sourceWalletId());
            assertNull(command.destWalletId());
            assertNull(command.countryCode());
        }
    }

    @Nested
    @DisplayName("When mapping FraudAssessmentCommand to AssessmentCommand")
    class WhenMappingCommandToUseCaseCommand {

        @Test
        @DisplayName("Should map all fields correctly")
        void shouldMapAllFieldsCorrectly() {
            UUID transactionId = UUID.randomUUID();
            UUID sourceWalletId = UUID.randomUUID();
            UUID destWalletId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            FraudAssessmentCommand command = new FraudAssessmentCommand(
                    transactionId, "TRANSFER", new BigDecimal("2000.00"), "USD",
                    sourceWalletId, destWalletId, userId, "US");

            AssessFraudUseCase.AssessmentCommand useCaseCommand = AssessmentMapper.toUseCaseCommand(command);

            assertEquals(transactionId, useCaseCommand.transactionId());
            assertEquals("TRANSFER", useCaseCommand.transactionType());
            assertEquals(new BigDecimal("2000.00"), useCaseCommand.amount());
            assertEquals("USD", useCaseCommand.currency());
            assertEquals(sourceWalletId, useCaseCommand.sourceWalletId());
            assertEquals(destWalletId, useCaseCommand.destWalletId());
            assertEquals(userId, useCaseCommand.userId());
            assertEquals("US", useCaseCommand.countryCode());
        }

        @Test
        @DisplayName("Should handle null optional fields")
        void shouldHandleNullOptionalFields() {
            FraudAssessmentCommand command = new FraudAssessmentCommand(
                    UUID.randomUUID(), "TRANSFER", BigDecimal.TEN, "EUR",
                    null, null, UUID.randomUUID(), null);

            AssessFraudUseCase.AssessmentCommand useCaseCommand = AssessmentMapper.toUseCaseCommand(command);

            assertNull(useCaseCommand.sourceWalletId());
            assertNull(useCaseCommand.destWalletId());
            assertNull(useCaseCommand.countryCode());
        }
    }
}
