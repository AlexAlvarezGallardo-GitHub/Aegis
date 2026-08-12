package com.aegis.payment.infrastructure.persistence;

import com.aegis.payment.domain.model.Payee;
import com.aegis.payment.domain.model.PayeeType;
import com.aegis.payment.domain.model.Payment;
import com.aegis.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentJpaEntity - Persistence Entity")
class PaymentJpaEntityTest {

    private static final UUID WALLET = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final String REFERENCE = "PAY-001";
    private static final Payee PAYEE = new Payee("Cafe Central", "merchant-123", PayeeType.MERCHANT);

    private Payment completedPayment() {
        Payment payment = Payment.request(WALLET, USER, new BigDecimal("25.00"), "EUR",
                PAYEE, "Coffee", REFERENCE);
        payment.startProcessing();
        payment.markFraudAssessed(UUID.randomUUID());
        payment.markFundsReserved(UUID.randomUUID());
        payment.complete();
        return payment;
    }

    @Nested
    @DisplayName("When mapping from domain Payment")
    class WhenMappingFromDomain {

        @Test
        @DisplayName("Should initialize all fields from the aggregate")
        void shouldInitializeAllFields() {
            Payment payment = completedPayment();

            PaymentJpaEntity entity = new PaymentJpaEntity(payment);

            assertEquals(payment.getId(), entity.getId());
            assertEquals(WALLET, entity.getWalletId());
            assertEquals(USER, entity.getUserId());
            assertEquals(new BigDecimal("25.00"), entity.getAmount());
            assertEquals("EUR", entity.getCurrency());
            assertEquals("Cafe Central", entity.getPayeeName());
            assertEquals("merchant-123", entity.getPayeeId());
            assertEquals(PayeeType.MERCHANT, entity.getPayeeType());
            assertEquals("Coffee", entity.getDescription());
            assertEquals(REFERENCE, entity.getReference());
            assertEquals(PaymentStatus.COMPLETED, entity.getStatus());
            assertNotNull(entity.getFraudAssessmentId());
            assertNotNull(entity.getHoldId());
            assertNull(entity.getFailureReason());
            assertNotNull(entity.getCompletedAt());
        }

        @Test
        @DisplayName("Should map nullable fields correctly for a pending payment")
        void shouldMapNullableFields() {
            Payment payment = Payment.request(WALLET, USER, new BigDecimal("25.00"), "EUR",
                    PAYEE, null, REFERENCE);

            PaymentJpaEntity entity = new PaymentJpaEntity(payment);

            assertEquals(PaymentStatus.PENDING, entity.getStatus());
            assertNull(entity.getFraudAssessmentId());
            assertNull(entity.getHoldId());
            assertNull(entity.getFailureReason());
            assertNull(entity.getCompletedAt());
            assertNull(entity.getDescription());
        }
    }

    @Nested
    @DisplayName("When converting back to domain Payment")
    class WhenConvertingToDomain {

        @Test
        @DisplayName("Should rehydrate a full completed payment")
        void shouldRehydrateCompletedPayment() {
            Payment original = completedPayment();
            PaymentJpaEntity entity = new PaymentJpaEntity(original);

            Payment rehydrated = entity.toDomain();

            assertEquals(original.getId(), rehydrated.getId());
            assertEquals(WALLET, rehydrated.getWalletId());
            assertEquals(USER, rehydrated.getUserId());
            assertEquals(original.getAmount(), rehydrated.getAmount());
            assertEquals("EUR", rehydrated.getCurrency());
            assertEquals(PAYEE, rehydrated.getPayee());
            assertEquals("Coffee", rehydrated.getDescription());
            assertEquals(REFERENCE, rehydrated.getReference());
            assertEquals(PaymentStatus.COMPLETED, rehydrated.getStatus());
            assertEquals(original.getFraudAssessmentId(), rehydrated.getFraudAssessmentId());
            assertEquals(original.getHoldId(), rehydrated.getHoldId());
            assertEquals(original.getCompletedAt(), rehydrated.getCompletedAt());
        }
    }
}
