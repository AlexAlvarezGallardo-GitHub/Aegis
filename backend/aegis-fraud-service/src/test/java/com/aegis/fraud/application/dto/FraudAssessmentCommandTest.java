package com.aegis.fraud.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FraudAssessmentCommand - Application Command")
class FraudAssessmentCommandTest {

    @Nested
    @DisplayName("When creating a command")
    class WhenCreatingACommand {

        @Test
        @DisplayName("Should store all fields correctly")
        void shouldStoreAllFieldsCorrectly() {
            UUID transactionId = UUID.randomUUID();
            UUID sourceWalletId = UUID.randomUUID();
            UUID destWalletId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            FraudAssessmentCommand command = new FraudAssessmentCommand(
                    transactionId, "TRANSFER", new BigDecimal("1500.50"), "EUR",
                    sourceWalletId, destWalletId, userId, "ES");

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
        @DisplayName("Should allow null optional fields")
        void shouldAllowNullOptionalFields() {
            FraudAssessmentCommand command = new FraudAssessmentCommand(
                    UUID.randomUUID(), "TRANSFER", BigDecimal.TEN, "EUR",
                    null, null, UUID.randomUUID(), null);

            assertNull(command.sourceWalletId());
            assertNull(command.destWalletId());
            assertNull(command.countryCode());
        }
    }
}
