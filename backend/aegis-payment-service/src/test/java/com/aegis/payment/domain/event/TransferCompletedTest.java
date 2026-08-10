package com.aegis.payment.domain.event;

import com.aegis.payment.domain.model.Transfer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for the TransferCompleted domain event.
 */
@DisplayName("TransferCompleted - Domain Event")
class TransferCompletedTest {

    @Nested
    @DisplayName("When constructing from a completed Transfer")
    class WhenConstructingFromTransfer {

        @Test
        @DisplayName("Should populate envelope and payload fields from the aggregate")
        void shouldPopulateFieldsFromTransfer() {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            Transfer transfer = Transfer.request(source, dest, user,
                    new BigDecimal("250.00"), "EUR", "payment", "ref-100");
            transfer.startFraudCheck();
            transfer.markFundsReserved(UUID.randomUUID());
            transfer.complete();

            TransferCompleted event = new TransferCompleted(transfer);

            assertNotNull(event.eventId());
            assertEquals(7, event.eventId().version());
            assertEquals("TRANSFER_COMPLETED", event.eventType());
            assertEquals("1.0", event.schemaVersion());
            assertNotNull(event.occurredAt());
            assertEquals(transfer.getId(), event.causationId());
            assertEquals(transfer.getId(), event.correlationId());
            assertEquals(transfer.getId(), event.aggregateId());
            assertEquals("TRANSFER", event.aggregateType());
            assertEquals(transfer.getId(), event.transferId());
            assertEquals(source, event.sourceWalletId());
            assertEquals(dest, event.destWalletId());
            assertEquals(user, event.userId());
            assertEquals(new BigDecimal("250.00"), event.amount());
            assertEquals("EUR", event.currency());
            assertNotNull(event.completedAt());
        }
    }

    @Nested
    @DisplayName("When constructing with canonical constructor")
    class WhenConstructingWithCanonical {

        @Test
        @DisplayName("Should retain all provided values")
        void shouldRetainAllValues() {
            UUID eventId = UUID.randomUUID();
            UUID transferId = UUID.randomUUID();
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            java.time.Instant now = java.time.Instant.now();

            TransferCompleted event = new TransferCompleted(
                    eventId, "TRANSFER_COMPLETED", "1.0", now,
                    transferId, transferId, transferId, "TRANSFER",
                    transferId, source, dest, user,
                    new BigDecimal("10.00"), "USD", now);

            assertEquals(eventId, event.eventId());
            assertEquals("TRANSFER_COMPLETED", event.eventType());
            assertEquals("1.0", event.schemaVersion());
            assertEquals(now, event.occurredAt());
            assertEquals("USD", event.currency());
        }
    }
}
