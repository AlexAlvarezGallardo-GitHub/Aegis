package com.aegis.audit.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditRecord - Domain Model")
class AuditRecordTest {

    @Test
    @DisplayName("Should create AuditRecord with generated UUIDv7")
    void shouldCreateWithGeneratedId() {
        UUID walletId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal newBalance = new BigDecimal("500.00");
        Instant eventTimestamp = Instant.now();
        Instant ingestedAt = Instant.now();

        AuditRecord record = AuditRecord.create(
                walletId, userId, amount, "USD", "BANK_TRANSFER",
                "REF-001", newBalance, eventTimestamp, ingestedAt, "corr-123"
        );

        assertNotNull(record.id());
        assertEquals(walletId, record.walletId());
        assertEquals(userId, record.userId());
        assertEquals(amount, record.amount());
        assertEquals("USD", record.currency());
        assertEquals("BANK_TRANSFER", record.source());
        assertEquals("REF-001", record.reference());
        assertEquals(newBalance, record.newBalance());
        assertEquals(eventTimestamp, record.eventTimestamp());
        assertEquals(ingestedAt, record.ingestedAt());
        assertEquals("corr-123", record.correlationId());
    }

    @Test
    @DisplayName("Should throw NullPointerException when walletId is null")
    void shouldThrowWhenWalletIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                AuditRecord.create(null, UUID.randomUUID(), BigDecimal.TEN, "USD",
                        "BANK_TRANSFER", "REF", BigDecimal.TEN, Instant.now(), Instant.now(), null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when userId is null")
    void shouldThrowWhenUserIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                AuditRecord.create(UUID.randomUUID(), null, BigDecimal.TEN, "USD",
                        "BANK_TRANSFER", "REF", BigDecimal.TEN, Instant.now(), Instant.now(), null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when amount is null")
    void shouldThrowWhenAmountIsNull() {
        assertThrows(NullPointerException.class, () ->
                AuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), null, "USD",
                        "BANK_TRANSFER", "REF", BigDecimal.TEN, Instant.now(), Instant.now(), null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when currency is null")
    void shouldThrowWhenCurrencyIsNull() {
        assertThrows(NullPointerException.class, () ->
                AuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, null,
                        "BANK_TRANSFER", "REF", BigDecimal.TEN, Instant.now(), Instant.now(), null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when newBalance is null")
    void shouldThrowWhenNewBalanceIsNull() {
        assertThrows(NullPointerException.class, () ->
                AuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "USD",
                        "BANK_TRANSFER", "REF", null, Instant.now(), Instant.now(), null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when eventTimestamp is null")
    void shouldThrowWhenEventTimestampIsNull() {
        assertThrows(NullPointerException.class, () ->
                AuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "USD",
                        "BANK_TRANSFER", "REF", BigDecimal.TEN, null, Instant.now(), null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when ingestedAt is null")
    void shouldThrowWhenIngestedAtIsNull() {
        assertThrows(NullPointerException.class, () ->
                AuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "USD",
                        "BANK_TRANSFER", "REF", BigDecimal.TEN, Instant.now(), null, null));
    }

    @Test
    @DisplayName("Should allow optional fields to be null")
    void shouldAllowOptionalFieldsNull() {
        AuditRecord record = AuditRecord.create(
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "USD",
                null, null, BigDecimal.TEN, Instant.now(), Instant.now(), null
        );

        assertNotNull(record);
        assertNull(record.source());
        assertNull(record.reference());
        assertNull(record.correlationId());
    }
}
