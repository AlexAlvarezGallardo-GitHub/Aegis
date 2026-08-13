package com.aegis.audit.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RefundAuditRecord - Domain Model")
class RefundAuditRecordTest {

    @Test
    @DisplayName("Should create RefundAuditRecord with generated UUIDv7")
    void shouldCreateWithGeneratedId() {
        UUID eventId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant eventTimestamp = Instant.now();
        Instant ingestedAt = Instant.now();

        RefundAuditRecord record = RefundAuditRecord.create(
                eventId, refundId, paymentId,
                walletId, userId,
                new BigDecimal("50.00"), "USD", "Customer request",
                "REF-001", "corr-123", eventTimestamp, ingestedAt
        );

        assertNotNull(record.id());
        assertEquals(eventId, record.eventId());
        assertEquals(refundId, record.refundId());
        assertEquals(paymentId, record.paymentId());
        assertEquals(walletId, record.walletId());
        assertEquals(userId, record.userId());
        assertEquals(new BigDecimal("50.00"), record.amount());
        assertEquals("USD", record.currency());
        assertEquals("Customer request", record.reason());
        assertEquals("REF-001", record.reference());
        assertEquals("corr-123", record.correlationId());
        assertEquals(eventTimestamp, record.eventTimestamp());
        assertEquals(ingestedAt, record.ingestedAt());
    }

    @Test
    @DisplayName("Should allow null reason")
    void shouldAllowNullReason() {
        RefundAuditRecord record = RefundAuditRecord.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("25.00"), "EUR", null,
                "REF-002", null, Instant.now(), Instant.now()
        );

        assertNotNull(record);
        assertNull(record.reason());
        assertNull(record.correlationId());
    }

    @Test
    @DisplayName("Should throw NullPointerException when eventId is null")
    void shouldThrowWhenEventIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                RefundAuditRecord.create(null, UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, "REF", null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when refundId is null")
    void shouldThrowWhenRefundIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                RefundAuditRecord.create(UUID.randomUUID(), null, UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, "REF", null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when paymentId is null")
    void shouldThrowWhenPaymentIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                RefundAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), null,
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, "REF", null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when walletId is null")
    void shouldThrowWhenWalletIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                RefundAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        null, UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, "REF", null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when userId is null")
    void shouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                RefundAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), null,
                        BigDecimal.TEN, "USD", null, "REF", null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when amount is null")
    void shouldThrowWhenAmountIsNull() {
        assertThrows(NullPointerException.class, () ->
                RefundAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(),
                        null, "USD", null, "REF", null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when currency is null")
    void shouldThrowWhenCurrencyIsNull() {
        assertThrows(NullPointerException.class, () ->
                RefundAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, null, null, "REF", null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when reference is null")
    void shouldThrowWhenReferenceIsNull() {
        assertThrows(NullPointerException.class, () ->
                RefundAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when eventTimestamp is null")
    void shouldThrowWhenEventTimestampIsNull() {
        assertThrows(NullPointerException.class, () ->
                RefundAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, "REF", null,
                        null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when ingestedAt is null")
    void shouldThrowWhenIngestedAtIsNull() {
        assertThrows(NullPointerException.class, () ->
                RefundAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", null, "REF", null,
                        Instant.now(), null));
    }
}
