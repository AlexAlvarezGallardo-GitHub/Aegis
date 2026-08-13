package com.aegis.payment.domain.model;

import com.aegis.payment.domain.exception.InvalidRefundStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Refund - Domain Model")
class RefundTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID WALLET_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("25.00");
    private static final String CURRENCY = "EUR";
    private static final String REFERENCE = "REF-001";

    @Nested
    @DisplayName("When creating a refund")
    class WhenCreatingRefund {

        @Test
        @DisplayName("Should generate UUID v7 refund ID")
        void shouldGenerateUuidV7Id() {
            Refund refund = Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, AMOUNT, CURRENCY, null, REFERENCE);
            assertNotNull(refund.getId());
            assertEquals(7, refund.getId().version());
        }

        @Test
        @DisplayName("Should set all fields correctly")
        void shouldSetAllFieldsCorrectly() {
            Refund refund = Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, AMOUNT, CURRENCY, "Product returned", REFERENCE);

            assertEquals(PAYMENT_ID, refund.getPaymentId());
            assertEquals(WALLET_ID, refund.getWalletId());
            assertEquals(USER_ID, refund.getUserId());
            assertEquals(new BigDecimal("25.00"), refund.getAmount());
            assertEquals(CURRENCY, refund.getCurrency());
            assertEquals("Product returned", refund.getReason());
            assertEquals(REFERENCE, refund.getReference());
            assertEquals(RefundStatus.PENDING, refund.getStatus());
            assertNotNull(refund.getCreatedAt());
            assertNotNull(refund.getUpdatedAt());
            assertNull(refund.getCompletedAt());
        }

        @Test
        @DisplayName("Should reject zero amount")
        void shouldRejectZeroAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, BigDecimal.ZERO, CURRENCY, null, REFERENCE));
        }

        @Test
        @DisplayName("Should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, new BigDecimal("-1.00"), CURRENCY, null, REFERENCE));
        }

        @Test
        @DisplayName("Should reject invalid currency")
        void shouldRejectInvalidCurrency() {
            assertThrows(IllegalArgumentException.class,
                    () -> Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, AMOUNT, "euro", null, REFERENCE));
        }

        @Test
        @DisplayName("Should reject blank reference")
        void shouldRejectBlankReference() {
            assertThrows(IllegalArgumentException.class,
                    () -> Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, AMOUNT, CURRENCY, null, "  "));
        }

        @Test
        @DisplayName("Should scale amount to 2 decimal places")
        void shouldScaleAmount() {
            Refund refund = Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, new BigDecimal("25"), CURRENCY, null, REFERENCE);
            assertEquals(new BigDecimal("25.00"), refund.getAmount());
        }
    }

    @Nested
    @DisplayName("State machine transitions")
    class StateMachineTransitions {

        @Test
        @DisplayName("PENDING -> COMPLETED via complete()")
        void pendingToCompleted() {
            Refund refund = Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, AMOUNT, CURRENCY, null, REFERENCE);
            refund.complete();
            assertEquals(RefundStatus.COMPLETED, refund.getStatus());
            assertNotNull(refund.getCompletedAt());
        }

        @Test
        @DisplayName("PENDING -> FAILED via fail()")
        void pendingToFailed() {
            Refund refund = Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, AMOUNT, CURRENCY, null, REFERENCE);
            refund.fail("WALLET_CREDIT_FAILED");
            assertEquals(RefundStatus.FAILED, refund.getStatus());
        }

        @Test
        @DisplayName("Invalid: COMPLETED -> FAILED throws")
        void invalidCompletedToFailed() {
            Refund refund = Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, AMOUNT, CURRENCY, null, REFERENCE);
            refund.complete();
            assertThrows(InvalidRefundStateException.class,
                    () -> refund.fail("too late"));
        }

        @Test
        @DisplayName("Invalid: FAILED -> COMPLETED throws")
        void invalidFailedToCompleted() {
            Refund refund = Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, AMOUNT, CURRENCY, null, REFERENCE);
            refund.fail("error");
            assertThrows(InvalidRefundStateException.class, refund::complete);
        }

        @Test
        @DisplayName("Invalid: double complete throws")
        void invalidDoubleComplete() {
            Refund refund = Refund.request(PAYMENT_ID, WALLET_ID, USER_ID, AMOUNT, CURRENCY, null, REFERENCE);
            refund.complete();
            assertThrows(InvalidRefundStateException.class, refund::complete);
        }
    }

    @Nested
    @DisplayName("RefundStatus enum")
    class RefundStatusEnum {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            RefundStatus[] values = RefundStatus.values();
            assertEquals(3, values.length);
            assertEquals(RefundStatus.PENDING, RefundStatus.valueOf("PENDING"));
            assertEquals(RefundStatus.COMPLETED, RefundStatus.valueOf("COMPLETED"));
            assertEquals(RefundStatus.FAILED, RefundStatus.valueOf("FAILED"));
        }
    }
}
