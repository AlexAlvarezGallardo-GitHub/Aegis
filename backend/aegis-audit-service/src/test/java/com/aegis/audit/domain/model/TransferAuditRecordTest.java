package com.aegis.audit.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransferAuditRecord - Domain Model")
class TransferAuditRecordTest {

    @Test
    @DisplayName("Should create TransferAuditRecord with generated UUIDv7")
    void shouldCreateWithGeneratedId() {
        UUID eventId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        UUID sourceWalletId = UUID.randomUUID();
        UUID destWalletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant eventTimestamp = Instant.now();
        Instant ingestedAt = Instant.now();

        TransferAuditRecord record = TransferAuditRecord.create(
                eventId, transferId, "REQUESTED",
                sourceWalletId, destWalletId, userId,
                new BigDecimal("100.00"), "USD", "REF-001",
                null, "corr-123", eventTimestamp, ingestedAt
        );

        assertNotNull(record.id());
        assertEquals(eventId, record.eventId());
        assertEquals(transferId, record.transferId());
        assertEquals("REQUESTED", record.eventType());
        assertEquals(sourceWalletId, record.sourceWalletId());
        assertEquals(destWalletId, record.destWalletId());
        assertEquals(userId, record.userId());
        assertEquals(new BigDecimal("100.00"), record.amount());
        assertEquals("USD", record.currency());
        assertEquals("REF-001", record.reference());
        assertNull(record.failureReason());
        assertEquals("corr-123", record.correlationId());
        assertEquals(eventTimestamp, record.eventTimestamp());
        assertEquals(ingestedAt, record.ingestedAt());
    }

    @Test
    @DisplayName("Should throw NullPointerException when eventId is null")
    void shouldThrowWhenEventIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferAuditRecord.create(null, UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", "ref", null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when transferId is null")
    void shouldThrowWhenTransferIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferAuditRecord.create(UUID.randomUUID(), null, "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", "ref", null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when eventType is null")
    void shouldThrowWhenEventTypeIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), null,
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", "ref", null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when sourceWalletId is null")
    void shouldThrowWhenSourceWalletIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        null, UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", "ref", null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when destWalletId is null")
    void shouldThrowWhenDestWalletIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), null, UUID.randomUUID(),
                        BigDecimal.TEN, "USD", "ref", null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when userId is null")
    void shouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(), null,
                        BigDecimal.TEN, "USD", "ref", null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when amount is null")
    void shouldThrowWhenAmountIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        null, "USD", "ref", null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when currency is null")
    void shouldThrowWhenCurrencyIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, null, "ref", null, null,
                        Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when eventTimestamp is null")
    void shouldThrowWhenEventTimestampIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", "ref", null, null,
                        null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when ingestedAt is null")
    void shouldThrowWhenIngestedAtIsNull() {
        assertThrows(NullPointerException.class, () ->
                TransferAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "REQUESTED",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "USD", "ref", null, null,
                        Instant.now(), null));
    }
}
