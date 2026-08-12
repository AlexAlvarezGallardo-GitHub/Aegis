package com.aegis.audit.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentAuditRecord - Domain Model")
class PaymentAuditRecordTest {

    @Test
    @DisplayName("Should create PaymentAuditRecord with generated UUIDv7")
    void shouldCreateWithGeneratedId() {
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant eventTimestamp = Instant.now();
        Instant ingestedAt = Instant.now();

        PaymentAuditRecord record = PaymentAuditRecord.create(
                eventId, paymentId, "REQUESTED",
                walletId, userId,
                new BigDecimal("100.00"), "USD", "Acme Corp",
                null, "corr-123", eventTimestamp, ingestedAt
        );

        assertNotNull(record.id());
        assertEquals(eventId, record.eventId());
        assertEquals(paymentId, record.paymentId());
        assertEquals("REQUESTED", record.eventType());
        assertEquals(walletId, record.walletId());
        assertEquals(userId, record.userId());
        assertEquals(new BigDecimal("100.00"), record.amount());
        assertEquals("USD", record.currency());
        assertEquals("Acme Corp", record.payeeName());
        assertNull(record.failureReason());
        assertEquals("corr-123", record.correlationId());
        assertEquals(eventTimestamp, record.eventTimestamp());
        assertEquals(ingestedAt, record.ingestedAt());
    }

    @Test
    @DisplayName("Should throw NullPointerException when eventId is null")
    void shouldThrowWhenEventIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                PaymentAuditRecord.create(null, UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when paymentId is null")
    void shouldThrowWhenPaymentIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                PaymentAuditRecord.create(UUID.randomUUID(), null, "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when eventType is null")
    void shouldThrowWhenEventTypeIsNull() {
        assertThrows(NullPointerException.class, () ->
                PaymentAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), null,
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when walletId is null")
    void shouldThrowWhenWalletIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                PaymentAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        null, UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when userId is null")
    void shouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                PaymentAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), null,
                        BigDecimal.TEN, "USD", null, null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when amount is null")
    void shouldThrowWhenAmountIsNull() {
        assertThrows(NullPointerException.class, () ->
                PaymentAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(),
                        null, "USD", null, null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when currency is null")
    void shouldThrowWhenCurrencyIsNull() {
        assertThrows(NullPointerException.class, () ->
                PaymentAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, null, null, null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when eventTimestamp is null")
    void shouldThrowWhenEventTimestampIsNull() {
        assertThrows(NullPointerException.class, () ->
                PaymentAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, null, null,
                        null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when ingestedAt is null")
    void shouldThrowWhenIngestedAtIsNull() {
        assertThrows(NullPointerException.class, () ->
                PaymentAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, null, null,
                        Instant.now(), null));
    }
}
