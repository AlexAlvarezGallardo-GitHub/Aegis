package com.aegis.payment.domain.model;

import com.aegis.payment.domain.exception.InvalidTransferStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Transfer - Domain Model")
class TransferTest {

    private static final UUID SOURCE = UUID.randomUUID();
    private static final UUID DEST = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final String CURRENCY = "EUR";
    private static final String REFERENCE = "ref-001";

    @Nested
    @DisplayName("When creating a transfer")
    class WhenCreatingTransfer {

        @Test
        @DisplayName("Should generate UUID v7 transfer ID")
        void shouldGenerateUuidV7Id() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            assertNotNull(transfer.getId());
            assertEquals(7, transfer.getId().version());
        }

        @Test
        @DisplayName("Should set all fields correctly")
        void shouldSetAllFieldsCorrectly() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, "desc", REFERENCE);

            assertEquals(SOURCE, transfer.getSourceWalletId());
            assertEquals(DEST, transfer.getDestWalletId());
            assertEquals(USER, transfer.getUserId());
            assertEquals(new BigDecimal("100.00"), transfer.getAmount());
            assertEquals(CURRENCY, transfer.getCurrency());
            assertEquals("desc", transfer.getDescription());
            assertEquals(REFERENCE, transfer.getReference());
            assertEquals(TransferStatus.PENDING, transfer.getStatus());
            assertNull(transfer.getFraudAssessmentId());
            assertNull(transfer.getHoldId());
            assertNull(transfer.getFailureReason());
            assertNotNull(transfer.getCreatedAt());
            assertNotNull(transfer.getUpdatedAt());
            assertNull(transfer.getCompletedAt());
        }

        @Test
        @DisplayName("Should reject zero amount")
        void shouldRejectZeroAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Transfer.request(SOURCE, DEST, USER, BigDecimal.ZERO, CURRENCY, null, REFERENCE));
        }

        @Test
        @DisplayName("Should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Transfer.request(SOURCE, DEST, USER, new BigDecimal("-1.00"), CURRENCY, null, REFERENCE));
        }

        @Test
        @DisplayName("Should reject invalid currency")
        void shouldRejectInvalidCurrency() {
            assertThrows(IllegalArgumentException.class,
                    () -> Transfer.request(SOURCE, DEST, USER, AMOUNT, "euro", null, REFERENCE));
        }

        @Test
        @DisplayName("Should reject lowercase currency")
        void shouldRejectLowercaseCurrency() {
            assertThrows(IllegalArgumentException.class,
                    () -> Transfer.request(SOURCE, DEST, USER, AMOUNT, "eur", null, REFERENCE));
        }

        @Test
        @DisplayName("Should reject same source and destination wallet")
        void shouldRejectSameWallets() {
            assertThrows(IllegalArgumentException.class,
                    () -> Transfer.request(SOURCE, SOURCE, USER, AMOUNT, CURRENCY, null, REFERENCE));
        }

        @Test
        @DisplayName("Should scale amount to 2 decimal places")
        void shouldScaleAmount() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, new BigDecimal("100"), CURRENCY, null, REFERENCE);
            assertEquals(new BigDecimal("100.00"), transfer.getAmount());
        }
    }

    @Nested
    @DisplayName("State machine transitions")
    class StateMachineTransitions {

        @Test
        @DisplayName("PENDING -> FRAUD_CHECK via startFraudCheck()")
        void pendingToFraudCheck() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            transfer.startFraudCheck();
            assertEquals(TransferStatus.FRAUD_CHECK, transfer.getStatus());
        }

        @Test
        @DisplayName("FRAUD_CHECK -> FUNDS_RESERVED via markFundsReserved()")
        void fraudCheckToFundsReserved() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            transfer.startFraudCheck();
            UUID holdId = UUID.randomUUID();
            transfer.markFundsReserved(holdId);
            assertEquals(TransferStatus.FUNDS_RESERVED, transfer.getStatus());
            assertEquals(holdId, transfer.getHoldId());
        }

        @Test
        @DisplayName("FUNDS_RESERVED -> COMPLETED via complete()")
        void fundsReservedToCompleted() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            transfer.startFraudCheck();
            transfer.markFundsReserved(UUID.randomUUID());
            transfer.complete();
            assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
            assertNotNull(transfer.getCompletedAt());
        }

        @Test
        @DisplayName("PENDING -> FAILED via fail()")
        void pendingToFailed() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            transfer.fail("insufficient funds");
            assertEquals(TransferStatus.FAILED, transfer.getStatus());
            assertEquals("insufficient funds", transfer.getFailureReason());
        }

        @Test
        @DisplayName("FRAUD_CHECK -> FAILED via fail()")
        void fraudCheckToFailed() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            transfer.startFraudCheck();
            transfer.fail("fraud detected");
            assertEquals(TransferStatus.FAILED, transfer.getStatus());
        }

        @Test
        @DisplayName("FUNDS_RESERVED -> FAILED via fail()")
        void fundsReservedToFailed() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            transfer.startFraudCheck();
            transfer.markFundsReserved(UUID.randomUUID());
            transfer.fail("settlement error");
            assertEquals(TransferStatus.FAILED, transfer.getStatus());
        }

        @Test
        @DisplayName("Invalid: PENDING -> FUNDS_RESERVED throws")
        void invalidPendingToFundsReserved() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            assertThrows(InvalidTransferStateException.class,
                    () -> transfer.markFundsReserved(UUID.randomUUID()));
        }

        @Test
        @DisplayName("Invalid: PENDING -> COMPLETED throws")
        void invalidPendingToCompleted() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            assertThrows(InvalidTransferStateException.class, transfer::complete);
        }

        @Test
        @DisplayName("Invalid: COMPLETED -> FAILED throws")
        void invalidCompletedToFailed() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            transfer.startFraudCheck();
            transfer.markFundsReserved(UUID.randomUUID());
            transfer.complete();
            assertThrows(InvalidTransferStateException.class,
                    () -> transfer.fail("too late"));
        }

        @Test
        @DisplayName("Invalid: FAILED -> COMPLETED throws")
        void invalidFailedToCompleted() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            transfer.fail("error");
            assertThrows(InvalidTransferStateException.class, transfer::complete);
        }

        @Test
        @DisplayName("Invalid: double startFraudCheck throws")
        void invalidDoubleStartFraudCheck() {
            Transfer transfer = Transfer.request(SOURCE, DEST, USER, AMOUNT, CURRENCY, null, REFERENCE);
            transfer.startFraudCheck();
            assertThrows(InvalidTransferStateException.class, transfer::startFraudCheck);
        }
    }

    @Nested
    @DisplayName("TransferStatus enum")
    class TransferStatusEnum {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            TransferStatus[] values = TransferStatus.values();
            assertEquals(6, values.length);
            assertEquals(TransferStatus.PENDING, TransferStatus.valueOf("PENDING"));
            assertEquals(TransferStatus.FRAUD_CHECK, TransferStatus.valueOf("FRAUD_CHECK"));
            assertEquals(TransferStatus.FUNDS_RESERVED, TransferStatus.valueOf("FUNDS_RESERVED"));
            assertEquals(TransferStatus.COMPLETED, TransferStatus.valueOf("COMPLETED"));
            assertEquals(TransferStatus.FAILED, TransferStatus.valueOf("FAILED"));
            assertEquals(TransferStatus.REVERSED, TransferStatus.valueOf("REVERSED"));
        }
    }
}
