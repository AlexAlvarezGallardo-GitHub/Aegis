package com.aegis.payment.domain.model;

import com.aegis.payment.domain.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Payment - Domain Model")
class PaymentTest {

    private static final UUID WALLET = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("25.00");
    private static final String CURRENCY = "EUR";
    private static final String REFERENCE = "PAY-001";
    private static final Payee PAYEE = new Payee("Cafe Central", "merchant-123", PayeeType.MERCHANT);

    @Nested
    @DisplayName("When creating a payment")
    class WhenCreatingPayment {

        @Test
        @DisplayName("Should generate UUID v7 payment ID")
        void shouldGenerateUuidV7Id() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            assertNotNull(payment.getId());
            assertEquals(7, payment.getId().version());
        }

        @Test
        @DisplayName("Should set all fields correctly")
        void shouldSetAllFieldsCorrectly() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, "Coffee", REFERENCE);

            assertEquals(WALLET, payment.getWalletId());
            assertEquals(USER, payment.getUserId());
            assertEquals(new BigDecimal("25.00"), payment.getAmount());
            assertEquals(CURRENCY, payment.getCurrency());
            assertEquals(PAYEE, payment.getPayee());
            assertEquals("Coffee", payment.getDescription());
            assertEquals(REFERENCE, payment.getReference());
            assertEquals(PaymentStatus.PENDING, payment.getStatus());
            assertNull(payment.getFraudAssessmentId());
            assertNull(payment.getHoldId());
            assertNull(payment.getFailureReason());
            assertNotNull(payment.getCreatedAt());
            assertNotNull(payment.getUpdatedAt());
            assertNull(payment.getCompletedAt());
        }

        @Test
        @DisplayName("Should reject zero amount")
        void shouldRejectZeroAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Payment.request(WALLET, USER, BigDecimal.ZERO, CURRENCY, PAYEE, null, REFERENCE));
        }

        @Test
        @DisplayName("Should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Payment.request(WALLET, USER, new BigDecimal("-1.00"), CURRENCY, PAYEE, null, REFERENCE));
        }

        @Test
        @DisplayName("Should reject invalid currency")
        void shouldRejectInvalidCurrency() {
            assertThrows(IllegalArgumentException.class,
                    () -> Payment.request(WALLET, USER, AMOUNT, "euro", PAYEE, null, REFERENCE));
        }

        @Test
        @DisplayName("Should reject blank reference")
        void shouldRejectBlankReference() {
            assertThrows(IllegalArgumentException.class,
                    () -> Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, "  "));
        }

        @Test
        @DisplayName("Should scale amount to 2 decimal places")
        void shouldScaleAmount() {
            Payment payment = Payment.request(WALLET, USER, new BigDecimal("25"), CURRENCY, PAYEE, null, REFERENCE);
            assertEquals(new BigDecimal("25.00"), payment.getAmount());
        }
    }

    @Nested
    @DisplayName("Payee value object")
    class PayeeValueObject {

        @Test
        @DisplayName("Should reject blank name")
        void shouldRejectBlankName() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Payee("  ", "id", PayeeType.MERCHANT));
        }

        @Test
        @DisplayName("Should reject blank id")
        void shouldRejectBlankId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Payee("name", "  ", PayeeType.MERCHANT));
        }

        @Test
        @DisplayName("Should have all PayeeType values")
        void shouldHaveAllPayeeTypes() {
            assertEquals(3, PayeeType.values().length);
            assertEquals(PayeeType.MERCHANT, PayeeType.valueOf("MERCHANT"));
            assertEquals(PayeeType.INDIVIDUAL, PayeeType.valueOf("INDIVIDUAL"));
            assertEquals(PayeeType.SERVICE, PayeeType.valueOf("SERVICE"));
        }
    }

    @Nested
    @DisplayName("State machine transitions")
    class StateMachineTransitions {

        @Test
        @DisplayName("PENDING -> PROCESSING via startProcessing()")
        void pendingToProcessing() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            payment.startProcessing();
            assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
        }

        @Test
        @DisplayName("PROCESSING -> COMPLETED via complete()")
        void processingToCompleted() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            payment.startProcessing();
            payment.markFundsReserved(UUID.randomUUID());
            payment.complete();
            assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
            assertNotNull(payment.getCompletedAt());
        }

        @Test
        @DisplayName("PENDING -> FAILED via fail()")
        void pendingToFailed() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            payment.fail("FRAUD_REJECTED");
            assertEquals(PaymentStatus.FAILED, payment.getStatus());
            assertEquals("FRAUD_REJECTED", payment.getFailureReason());
        }

        @Test
        @DisplayName("PROCESSING -> FAILED via fail()")
        void processingToFailed() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            payment.startProcessing();
            payment.fail("SETTLEMENT_FAILED");
            assertEquals(PaymentStatus.FAILED, payment.getStatus());
        }

        @Test
        @DisplayName("Invalid: PENDING -> COMPLETED throws")
        void invalidPendingToCompleted() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            assertThrows(InvalidPaymentStateException.class, payment::complete);
        }

        @Test
        @DisplayName("Invalid: COMPLETED -> FAILED throws")
        void invalidCompletedToFailed() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            payment.startProcessing();
            payment.markFundsReserved(UUID.randomUUID());
            payment.complete();
            assertThrows(InvalidPaymentStateException.class,
                    () -> payment.fail("too late"));
        }

        @Test
        @DisplayName("Invalid: FAILED -> COMPLETED throws")
        void invalidFailedToCompleted() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            payment.fail("error");
            assertThrows(InvalidPaymentStateException.class, payment::complete);
        }

        @Test
        @DisplayName("Invalid: double startProcessing throws")
        void invalidDoubleStartProcessing() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            payment.startProcessing();
            assertThrows(InvalidPaymentStateException.class, payment::startProcessing);
        }

        @Test
        @DisplayName("COMPLETED -> REFUNDED via markRefunded()")
        void completedToRefunded() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            payment.startProcessing();
            payment.markFundsReserved(UUID.randomUUID());
            payment.complete();
            payment.markRefunded();
            assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        }

        @Test
        @DisplayName("Invalid: PENDING -> REFUNDED throws")
        void invalidPendingToRefunded() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            assertThrows(InvalidPaymentStateException.class, payment::markRefunded);
        }

        @Test
        @DisplayName("Invalid: FAILED -> REFUNDED throws")
        void invalidFailedToRefunded() {
            Payment payment = Payment.request(WALLET, USER, AMOUNT, CURRENCY, PAYEE, null, REFERENCE);
            payment.fail("error");
            assertThrows(InvalidPaymentStateException.class, payment::markRefunded);
        }
    }

    @Nested
    @DisplayName("PaymentStatus enum")
    class PaymentStatusEnum {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            PaymentStatus[] values = PaymentStatus.values();
            assertEquals(5, values.length);
            assertEquals(PaymentStatus.PENDING, PaymentStatus.valueOf("PENDING"));
            assertEquals(PaymentStatus.PROCESSING, PaymentStatus.valueOf("PROCESSING"));
            assertEquals(PaymentStatus.COMPLETED, PaymentStatus.valueOf("COMPLETED"));
            assertEquals(PaymentStatus.FAILED, PaymentStatus.valueOf("FAILED"));
            assertEquals(PaymentStatus.REFUNDED, PaymentStatus.valueOf("REFUNDED"));
        }
    }
}
