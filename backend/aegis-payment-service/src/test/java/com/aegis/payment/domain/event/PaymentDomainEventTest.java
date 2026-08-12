package com.aegis.payment.domain.event;

import com.aegis.payment.domain.model.Payee;
import com.aegis.payment.domain.model.PayeeType;
import com.aegis.payment.domain.model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentDomainEvents - Envelope and Payload")
class PaymentDomainEventTest {

    private static final UUID WALLET = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final String REFERENCE = "PAY-001";
    private static final Payee PAYEE = new Payee("Cafe Central", "merchant-123", PayeeType.MERCHANT);

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.request(WALLET, USER, new BigDecimal("25.00"), "EUR", PAYEE, null, REFERENCE);
    }

    @Test
    @DisplayName("PaymentRequested should derive all envelope and payload fields from the aggregate")
    void paymentRequestedEnvelope() {
        PaymentRequested event = new PaymentRequested(payment);

        assertEquals("PAYMENT_REQUESTED", event.eventType());
        assertEquals("1.0", event.schemaVersion());
        assertEquals("PAYMENT", event.aggregateType());
        assertEquals(payment.getId(), event.aggregateId());
        assertEquals(payment.getId(), event.paymentId());
        assertEquals(WALLET, event.walletId());
        assertEquals(USER, event.userId());
        assertEquals(new BigDecimal("25.00"), event.amount());
        assertEquals("EUR", event.currency());
        assertEquals(PAYEE, event.payee());
        assertEquals(REFERENCE, event.reference());
        assertNotNull(event.eventId());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("PaymentExecuted should derive all envelope and payload fields from the aggregate")
    void paymentExecutedEnvelope() {
        payment.startProcessing();
        payment.markFraudAssessed(UUID.randomUUID());
        payment.markFundsReserved(UUID.randomUUID());
        payment.complete();

        PaymentExecuted event = new PaymentExecuted(payment);

        assertEquals("PAYMENT_EXECUTED", event.eventType());
        assertEquals("1.0", event.schemaVersion());
        assertEquals("PAYMENT", event.aggregateType());
        assertEquals(payment.getId(), event.aggregateId());
        assertEquals(payment.getId(), event.paymentId());
        assertEquals(WALLET, event.walletId());
        assertEquals(USER, event.userId());
        assertEquals(new BigDecimal("25.00"), event.amount());
        assertEquals("EUR", event.currency());
        assertEquals(PAYEE, event.payee());
        assertEquals(REFERENCE, event.reference());
        assertEquals(payment.getCompletedAt(), event.completedAt());
        assertNotNull(event.eventId());
    }

    @Test
    @DisplayName("PaymentFailed should derive all envelope and payload fields from the aggregate")
    void paymentFailedEnvelope() {
        payment.fail("FRAUD_REJECTED");

        PaymentFailed event = new PaymentFailed(payment, false);

        assertEquals("PAYMENT_FAILED", event.eventType());
        assertEquals("1.0", event.schemaVersion());
        assertEquals("PAYMENT", event.aggregateType());
        assertEquals(payment.getId(), event.aggregateId());
        assertEquals(payment.getId(), event.paymentId());
        assertEquals(WALLET, event.walletId());
        assertEquals("FRAUD_REJECTED", event.failureReason());
        assertFalse(event.compensated());
        assertNotNull(event.eventId());
    }

    @Test
    @DisplayName("Each event instance should carry a unique eventId")
    void uniqueEventIds() {
        PaymentRequested first = new PaymentRequested(payment);
        PaymentRequested second = new PaymentRequested(payment);

        assertNotEquals(first.eventId(), second.eventId());
    }
}
