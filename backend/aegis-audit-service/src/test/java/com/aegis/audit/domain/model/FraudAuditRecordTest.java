package com.aegis.audit.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FraudAuditRecord - Domain Model")
class FraudAuditRecordTest {

    @Test
    @DisplayName("Should create FraudAuditRecord with generated UUIDv7")
    void shouldCreateWithGeneratedId() {
        UUID assessmentId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Instant eventTimestamp = Instant.now();
        Instant ingestedAt = Instant.now();

        FraudAuditRecord record = FraudAuditRecord.create(
                assessmentId, transactionId, "TRANSFER", 75,
                "REVIEW", "[{\"rule\":\"test\"}]", eventTimestamp, ingestedAt
        );

        assertNotNull(record.id());
        assertEquals(assessmentId, record.assessmentId());
        assertEquals(transactionId, record.transactionId());
        assertEquals("TRANSFER", record.transactionType());
        assertEquals(75, record.riskScore());
        assertEquals("REVIEW", record.decision());
        assertEquals("[{\"rule\":\"test\"}]", record.rulesEvaluated());
        assertEquals(eventTimestamp, record.eventTimestamp());
        assertEquals(ingestedAt, record.ingestedAt());
    }

    @Test
    @DisplayName("Should throw NullPointerException when assessmentId is null")
    void shouldThrowWhenAssessmentIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                FraudAuditRecord.create(null, UUID.randomUUID(), "TRANSFER", 50,
                        "APPROVED", "[]", Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when transactionId is null")
    void shouldThrowWhenTransactionIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                FraudAuditRecord.create(UUID.randomUUID(), null, "TRANSFER", 50,
                        "APPROVED", "[]", Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when transactionType is null")
    void shouldThrowWhenTransactionTypeIsNull() {
        assertThrows(NullPointerException.class, () ->
                FraudAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), null, 50,
                        "APPROVED", "[]", Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when decision is null")
    void shouldThrowWhenDecisionIsNull() {
        assertThrows(NullPointerException.class, () ->
                FraudAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 50,
                        null, "[]", Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when rulesEvaluated is null")
    void shouldThrowWhenRulesEvaluatedIsNull() {
        assertThrows(NullPointerException.class, () ->
                FraudAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 50,
                        "APPROVED", null, Instant.now(), Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when eventTimestamp is null")
    void shouldThrowWhenEventTimestampIsNull() {
        assertThrows(NullPointerException.class, () ->
                FraudAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 50,
                        "APPROVED", "[]", null, Instant.now()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when ingestedAt is null")
    void shouldThrowWhenIngestedAtIsNull() {
        assertThrows(NullPointerException.class, () ->
                FraudAuditRecord.create(UUID.randomUUID(), UUID.randomUUID(), "TRANSFER", 50,
                        "APPROVED", "[]", Instant.now(), null));
    }
}
