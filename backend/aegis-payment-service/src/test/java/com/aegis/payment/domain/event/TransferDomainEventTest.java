package com.aegis.payment.domain.event;

import com.aegis.payment.domain.model.Transfer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransferDomainEvents - Envelope and Payload")
class TransferDomainEventTest {

    private static final UUID SOURCE = UUID.randomUUID();
    private static final UUID DEST = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final String REFERENCE = "ref-001";

    private Transfer requested;

    @BeforeEach
    void setUp() {
        requested = Transfer.request(SOURCE, DEST, USER, new BigDecimal("100.00"), "EUR", null, REFERENCE);
    }

    @Test
    @DisplayName("TransferRequested should derive all envelope and payload fields from the aggregate")
    void transferRequestedEnvelope() {
        TransferRequested event = new TransferRequested(requested);

        assertEquals("TRANSFER_REQUESTED", event.eventType());
        assertEquals("1.0", event.schemaVersion());
        assertEquals("TRANSFER", event.aggregateType());
        assertEquals(requested.getId(), event.aggregateId());
        assertEquals(requested.getId(), event.transferId());
        assertEquals(requested.getId(), event.causationId());
        assertEquals(requested.getId(), event.correlationId());
        assertEquals(SOURCE, event.sourceWalletId());
        assertEquals(DEST, event.destWalletId());
        assertEquals(USER, event.userId());
        assertEquals(new BigDecimal("100.00"), event.amount());
        assertEquals("EUR", event.currency());
        assertEquals(REFERENCE, event.reference());
        assertNotNull(event.eventId());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("TransferCompleted should derive all envelope and payload fields from the aggregate")
    void transferCompletedEnvelope() {
        requested.startFraudCheck();
        requested.markFundsReserved(UUID.randomUUID());
        requested.complete();

        TransferCompleted event = new TransferCompleted(requested);

        assertEquals("TRANSFER_COMPLETED", event.eventType());
        assertEquals("1.0", event.schemaVersion());
        assertEquals("TRANSFER", event.aggregateType());
        assertEquals(requested.getId(), event.aggregateId());
        assertEquals(requested.getId(), event.transferId());
        assertEquals(SOURCE, event.sourceWalletId());
        assertEquals(DEST, event.destWalletId());
        assertEquals(USER, event.userId());
        assertEquals(new BigDecimal("100.00"), event.amount());
        assertEquals("EUR", event.currency());
        assertEquals(requested.getCompletedAt(), event.completedAt());
        assertNotNull(event.eventId());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("TransferFailed should derive all envelope and payload fields from the aggregate")
    void transferFailedEnvelope() {
        requested.fail("INSUFFICIENT_FUNDS");

        TransferFailed event = new TransferFailed(requested);

        assertEquals("TRANSFER_FAILED", event.eventType());
        assertEquals("1.0", event.schemaVersion());
        assertEquals("TRANSFER", event.aggregateType());
        assertEquals(requested.getId(), event.aggregateId());
        assertEquals(requested.getId(), event.transferId());
        assertEquals(SOURCE, event.sourceWalletId());
        assertEquals(DEST, event.destWalletId());
        assertEquals(USER, event.userId());
        assertEquals(new BigDecimal("100.00"), event.amount());
        assertEquals("EUR", event.currency());
        assertEquals("INSUFFICIENT_FUNDS", event.failureReason());
        assertNotNull(event.eventId());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("Each event instance should carry a unique eventId")
    void uniqueEventIds() {
        TransferRequested first = new TransferRequested(requested);
        TransferRequested second = new TransferRequested(requested);

        assertNotEquals(first.eventId(), second.eventId());
    }
}
